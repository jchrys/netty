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
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.CombinedChannelDuplexHandler;
import io.netty5.channel.internal.DelegatingChannelHandlerContext;

import java.util.ArrayDeque;
import java.util.Queue;

import static io.netty5.handler.codec.http.HttpObjectDecoder.DEFAULT_MAX_HEADER_SIZE;
import static io.netty5.handler.codec.http.HttpObjectDecoder.DEFAULT_MAX_INITIAL_LINE_LENGTH;

/**
 * A combination of {@link HttpRequestDecoder} and {@link HttpResponseEncoder}
 * which enables easier server side HTTP implementation.
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
 * @see HttpClientCodec
 */
public final class HttpServerCodec extends CombinedChannelDuplexHandler<HttpRequestDecoder, HttpResponseEncoder>
        implements HttpServerUpgradeHandler.SourceCodec {

    /** A queue that is used for correlating a request and a response. */
    private final Queue<HttpMethod> queue = new ArrayDeque<>();

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength} ({@value HttpObjectDecoder#DEFAULT_MAX_INITIAL_LINE_LENGTH}),
     * {@code maxHeaderSize} ({@value HttpObjectDecoder#DEFAULT_MAX_HEADER_SIZE}),
     * and {@code chunkedSupported} ({@value HttpObjectDecoder#DEFAULT_CHUNKED_SUPPORTED}).
     */
    public HttpServerCodec() {
        this(DEFAULT_MAX_INITIAL_LINE_LENGTH, DEFAULT_MAX_HEADER_SIZE);
    }

    /**
     * Creates a new instance with the specified decoder options.
     */
    public HttpServerCodec(int maxInitialLineLength, int maxHeaderSize) {
        init(new HttpServerRequestDecoder(maxInitialLineLength, maxHeaderSize),
                new HttpServerResponseEncoder());
    }

    /**
     * Creates a new instance with the specified decoder configuration.
     */
    public HttpServerCodec(HttpDecoderConfig config) {
        init(new HttpServerRequestDecoder(config),
                new HttpServerResponseEncoder());
    }

    /**
     * Upgrades to another protocol from HTTP. Removes the {@link HttpRequestDecoder} and
     * {@link HttpResponseEncoder} from the pipeline.
     */
    @Override
    public void upgradeFrom(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    private final class HttpServerRequestDecoder extends HttpRequestDecoder {

        private ChannelHandlerContext context;

        HttpServerRequestDecoder(int maxInitialLineLength, int maxHeaderSize) {
            super(maxInitialLineLength, maxHeaderSize);
        }

        HttpServerRequestDecoder(HttpDecoderConfig config) {
            super(config);
        }

        @Override
        protected void decode(final ChannelHandlerContext ctx, Buffer buffer) throws Exception {
            super.decode(context, buffer);
        }

        @Override
        protected void handlerAdded0(final ChannelHandlerContext ctx) {
            context = new DelegatingChannelHandlerContext(ctx) {

                @Override
                public ChannelHandlerContext fireChannelRead(Object msg) {
                    if (msg instanceof HttpRequest) {
                        queue.add(((HttpRequest) msg).method());
                    }
                    super.fireChannelRead(msg);
                    return this;
                }
            };
        }
    }

    private final class HttpServerResponseEncoder extends HttpResponseEncoder {

        private HttpMethod method;

        @Override
        protected void sanitizeHeadersBeforeEncode(HttpResponse msg, boolean isAlwaysEmpty) {
            if (!isAlwaysEmpty && HttpMethod.CONNECT.equals(method)
                    && msg.status().codeClass() == HttpStatusClass.SUCCESS) {
                // Stripping Transfer-Encoding:
                // See https://tools.ietf.org/html/rfc7230#section-3.3.1
                msg.headers().remove(HttpHeaderNames.TRANSFER_ENCODING);
                return;
            }

            super.sanitizeHeadersBeforeEncode(msg, isAlwaysEmpty);
        }

        @Override
        protected boolean isContentAlwaysEmpty(@SuppressWarnings("unused") HttpResponse msg) {
            method = queue.poll();
            return HttpMethod.HEAD.equals(method) || super.isContentAlwaysEmpty(msg);
        }
    }
}
