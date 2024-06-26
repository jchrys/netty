/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty5.handler.codec.http;

import io.netty5.buffer.Buffer;
import io.netty5.channel.Channel;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelPipeline;
import io.netty5.channel.CombinedChannelDuplexHandler;
import io.netty5.channel.internal.DelegatingChannelHandlerContext;
import io.netty5.handler.codec.PrematureChannelClosureException;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A combination of {@link HttpRequestEncoder} and {@link HttpResponseDecoder}
 * which enables easier client side HTTP implementation. {@link HttpClientCodec}
 * provides additional state management for <tt>HEAD</tt> and <tt>CONNECT</tt>
 * requests, which {@link HttpResponseDecoder} lacks.  Please refer to
 * {@link HttpResponseDecoder} to learn what additional state management needs
 * to be done for <tt>HEAD</tt> and <tt>CONNECT</tt> and why
 * {@link HttpResponseDecoder} can not handle it by itself.
 * <p>
 * If the {@link Channel} is closed and there are missing responses,
 * a {@link PrematureChannelClosureException} is thrown.
 *
 * <h3>Header Validation</h3>
 *
 * It is recommended to always enable header validation.
 * <p>
 * Without header validation, your system can become vulnerable to
 * <a href="https://cwe.mitre.org/data/definitions/113.html">
 *     CWE-113: Improper Neutralization of CRLF Sequences in HTTP Headers ('HTTP Response Splitting')
 * </a>.
 * <p>
 * This recommendation stands even when both peers in the HTTP exchange are trusted,
 * as it helps with defence-in-depth.
 *
 * @see HttpServerCodec
 */
public final class HttpClientCodec extends CombinedChannelDuplexHandler<HttpResponseDecoder, HttpRequestEncoder>
        implements HttpClientUpgradeHandler.SourceCodec {
    public static final boolean DEFAULT_FAIL_ON_MISSING_RESPONSE = false;
    public static final boolean DEFAULT_PARSE_HTTP_AFTER_CONNECT_REQUEST = false;

    /** A queue that is used for correlating a request and a response. */
    private final Queue<HttpMethod> queue = new ArrayDeque<>();
    private final boolean parseHttpAfterConnectRequest;

    /** If true, decoding stops (i.e. pass-through) */
    private boolean done;

    private final AtomicLong requestResponseCounter = new AtomicLong();
    private final boolean failOnMissingResponse;

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength} ({@value HttpObjectDecoder#DEFAULT_MAX_INITIAL_LINE_LENGTH}),
     * {@code maxHeaderSize} ({@value HttpObjectDecoder#DEFAULT_MAX_HEADER_SIZE}),
     * and {@code chunkedSupported} ({@value HttpObjectDecoder#DEFAULT_CHUNKED_SUPPORTED}).
     */
    public HttpClientCodec() {
        this(new HttpDecoderConfig());
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    public HttpClientCodec(HttpDecoderConfig config) {
        this(config, DEFAULT_FAIL_ON_MISSING_RESPONSE, DEFAULT_PARSE_HTTP_AFTER_CONNECT_REQUEST);
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    public HttpClientCodec(HttpDecoderConfig config, boolean failOnMissingResponse) {
        this(config, failOnMissingResponse, DEFAULT_PARSE_HTTP_AFTER_CONNECT_REQUEST);
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    public HttpClientCodec(
            HttpDecoderConfig config, boolean failOnMissingResponse, boolean parseHttpAfterConnectRequest) {
        init(new Decoder(config), new Encoder());
        this.parseHttpAfterConnectRequest = parseHttpAfterConnectRequest;
        this.failOnMissingResponse = failOnMissingResponse;
    }

    /**
     * Prepares to upgrade to another protocol from HTTP. Disables the {@link Encoder}.
     */
    @Override
    public void prepareUpgradeFrom(ChannelHandlerContext ctx) {
        ((Encoder) outboundHandler()).upgraded = true;
    }

    /**
     * Upgrades to another protocol from HTTP. Removes the {@link Decoder} and {@link Encoder} from
     * the pipeline.
     */
    @Override
    public void upgradeFrom(ChannelHandlerContext ctx) {
        final ChannelPipeline p = ctx.pipeline();
        p.remove(this);
    }

    public void setSingleDecode(boolean singleDecode) {
        inboundHandler().setSingleDecode(singleDecode);
    }

    public boolean isSingleDecode() {
        return inboundHandler().isSingleDecode();
    }

    private final class Encoder extends HttpRequestEncoder {

        boolean upgraded;

        @Override
        protected void encodeAndClose(
                ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

            if (upgraded) {
                out.add(msg);
                return;
            }

            if (msg instanceof HttpRequest) {
                queue.offer(((HttpRequest) msg).method());
            }

            super.encodeAndClose(ctx, msg, out);

            if (failOnMissingResponse && !done) {
                // check if the request is chunked if so do not increment
                if (msg instanceof LastHttpContent) {
                    // increment as its the last chunk
                    requestResponseCounter.incrementAndGet();
                }
            }
        }
    }

    private final class Decoder extends HttpResponseDecoder {

        private ChannelHandlerContext context;

        Decoder(HttpDecoderConfig config) {
            super(config);
        }

        @Override
        protected void handlerAdded0(ChannelHandlerContext ctx) {
            if (failOnMissingResponse) {
                context = new DelegatingChannelHandlerContext(ctx) {
                   @Override
                   public ChannelHandlerContext fireChannelRead(Object msg) {
                       decrement(msg);

                       super.fireChannelRead(msg);
                       return this;
                   }
                };
            } else {
                context = ctx;
            }
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, Buffer buffer) throws Exception {
            if (done) {
                int readable = actualReadableBytes();
                if (readable == 0) {
                    // if non is readable just return null
                    // https://github.com/netty/netty/issues/1159
                    return;
                }
                ctx.fireChannelRead(buffer.readSplit(readable));
            } else {
                super.decode(context, buffer);
            }
        }

        private void decrement(Object msg) {
            if (msg == null) {
                return;
            }

            // check if it's an Header and its transfer encoding is not chunked.
            if (msg instanceof LastHttpContent) {
                requestResponseCounter.decrementAndGet();
            }
        }

        @Override
        protected boolean isContentAlwaysEmpty(HttpMessage msg) {
            // Get the method of the HTTP request that corresponds to the
            // current response.
            //
            // Even if we do not use the method to compare we still need to poll it to ensure we keep
            // request / response pairs in sync.
            HttpMethod method = queue.poll();

            final HttpResponseStatus status = ((HttpResponse) msg).status();
            final HttpStatusClass statusClass = status.codeClass();
            final int statusCode = status.code();
            if (statusClass == HttpStatusClass.INFORMATIONAL) {
                // An informational response should be excluded from paired comparison.
                // Just delegate to super method which has all the needed handling.
                return super.isContentAlwaysEmpty(msg);
            }

            // If the remote peer did for example send multiple responses for one request (which is not allowed per
            // spec but may still be possible) method will be null so guard against it.
            if (method != null) {
                char firstChar = method.name().charAt(0);
                switch (firstChar) {
                    case 'H':
                        // According to 4.3, RFC2616:
                        // All responses to the HEAD request method MUST NOT include a
                        // message-body, even though the presence of entity-header fields
                        // might lead one to believe they do.
                        if (HttpMethod.HEAD.equals(method)) {
                            return true;

                            // The following code was inserted to work around the servers
                            // that behave incorrectly.  It has been commented out
                            // because it does not work with well behaving servers.
                            // Please note, even if the 'Transfer-Encoding: chunked'
                            // header exists in the HEAD response, the response should
                            // have absolutely no content.
                            //
                            //// Interesting edge case:
                            //// Some poorly implemented servers will send a zero-byte
                            //// chunk if Transfer-Encoding of the response is 'chunked'.
                            ////
                            //// return !msg.isChunked();
                        }
                        break;
                    case 'C':
                        // Successful CONNECT request results in a response with empty body.
                        if (statusCode == 200) {
                            if (HttpMethod.CONNECT.equals(method)) {
                                // Proxy connection established - Parse HTTP only if configured by
                                // parseHttpAfterConnectRequest, else pass through.
                                if (!parseHttpAfterConnectRequest) {
                                    done = true;
                                    queue.clear();
                                }
                                return true;
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
            return super.isContentAlwaysEmpty(msg);
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx)
                throws Exception {
            super.channelInactive(ctx);

            if (failOnMissingResponse) {
                long missingResponses = requestResponseCounter.get();
                if (missingResponses > 0) {
                    ctx.fireChannelExceptionCaught(new PrematureChannelClosureException(
                            "channel gone inactive with " + missingResponses +
                            " missing response(s)"));
                }
            }
        }
    }
}
