/*
 * Copyright 2013 The Netty Project
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
package io.netty5.handler.codec.http.websocketx;

import io.netty5.channel.ChannelHandler;
import io.netty5.channel.ChannelHandlerContext;
import io.netty5.channel.ChannelPipeline;
import io.netty5.handler.codec.http.headers.HttpHeaders;
import io.netty5.util.Resource;

import java.net.URI;
import java.util.Objects;

import static io.netty5.handler.codec.http.websocketx.WebSocketClientProtocolConfig.DEFAULT_ALLOW_MASK_MISMATCH;
import static io.netty5.handler.codec.http.websocketx.WebSocketClientProtocolConfig.DEFAULT_DROP_PONG_FRAMES;
import static io.netty5.handler.codec.http.websocketx.WebSocketClientProtocolConfig.DEFAULT_HANDLE_CLOSE_FRAMES;
import static io.netty5.handler.codec.http.websocketx.WebSocketClientProtocolConfig.DEFAULT_PERFORM_MASKING;
import static io.netty5.handler.codec.http.websocketx.WebSocketServerProtocolConfig.DEFAULT_HANDSHAKE_TIMEOUT_MILLIS;
import static io.netty5.handler.codec.http.websocketx.WebSocketClientProtocolConfig.DEFAULT_WITH_UTF8_VALIDATOR;

/**
 * This handler does all the heavy lifting for you to run a websocket client.
 *
 * It takes care of websocket handshaking as well as processing of Ping, Pong frames. Text and Binary
 * data frames are passed to the next handler in the pipeline (implemented by you) for processing.
 * Also the close frame is passed to the next handler as you may want inspect it before close the connection if
 * the {@code handleCloseFrames} is {@code false}, default is {@code true}.
 *
 * This implementation will establish the websocket connection once the connection to the remote server was complete.
 *
 * To know once a handshake was done you can intercept the
 * {@link ChannelHandler#channelInboundEvent(ChannelHandlerContext, Object)} and check if the event was of type
 * {@link WebSocketHandshakeCompletionEvent}.
 */
public class WebSocketClientProtocolHandler extends WebSocketProtocolHandler {
    private final WebSocketClientHandshaker handshaker;
    private final WebSocketClientProtocolConfig clientConfig;

    /**
     * Returns the used handshaker
     */
    public WebSocketClientHandshaker handshaker() {
        return handshaker;
    }

    /**
     * Base constructor
     *
     * @param clientConfig
     *            Client protocol configuration.
     */
    public WebSocketClientProtocolHandler(WebSocketClientProtocolConfig clientConfig) {
        super(Objects.requireNonNull(clientConfig, "clientConfig").dropPongFrames(),
              clientConfig.sendCloseFrame(), clientConfig.forceCloseTimeoutMillis());
        this.handshaker = WebSocketClientHandshakerFactory.newHandshaker(
            clientConfig.webSocketUri(),
            clientConfig.version(),
            clientConfig.subprotocol(),
            clientConfig.allowExtensions(),
            clientConfig.customHeaders(),
            clientConfig.maxFramePayloadLength(),
            clientConfig.performMasking(),
            clientConfig.allowMaskMismatch(),
            clientConfig.forceCloseTimeoutMillis(),
            clientConfig.absoluteUpgradeUrl(),
            clientConfig.generateOriginHeader()
        );
        this.clientConfig = clientConfig;
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param clientConfig
     *            Client protocol configuration.
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker,
                                          WebSocketClientProtocolConfig clientConfig) {
        super(Objects.requireNonNull(clientConfig, "clientConfig").dropPongFrames(),
              clientConfig.sendCloseFrame(), clientConfig.forceCloseTimeoutMillis());
        this.handshaker = handshaker;
        this.clientConfig = clientConfig;
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param performMasking
     *            Whether to mask all written websocket frames. This must be set to true in order to be fully compatible
     *            with the websocket specifications. Client applications that communicate with a non-standard server
     *            which doesn't require masking might set this to false to achieve a higher performance.
     * @param allowMaskMismatch
     *            When set to true, frames which are not masked properly according to the standard will still be
     *            accepted.
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                          boolean allowExtensions, HttpHeaders customHeaders,
                                          int maxFramePayloadLength, boolean handleCloseFrames,
                                          boolean performMasking, boolean allowMaskMismatch) {
        this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength,
            handleCloseFrames, performMasking, allowMaskMismatch, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param performMasking
     *            Whether to mask all written websocket frames. This must be set to true in order to be fully compatible
     *            with the websocket specifications. Client applications that communicate with a non-standard server
     *            which doesn't require masking might set this to false to achieve a higher performance.
     * @param allowMaskMismatch
     *            When set to true, frames which are not masked properly according to the standard will still be
     *            accepted.
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                          boolean allowExtensions, HttpHeaders customHeaders,
                                          int maxFramePayloadLength, boolean handleCloseFrames, boolean performMasking,
                                          boolean allowMaskMismatch, long handshakeTimeoutMillis) {
        this(WebSocketClientHandshakerFactory.newHandshaker(webSocketURL, version, subprotocol,
                                                            allowExtensions, customHeaders, maxFramePayloadLength,
                                                            performMasking, allowMaskMismatch),
             handleCloseFrames, handshakeTimeoutMillis);
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                                   boolean allowExtensions, HttpHeaders customHeaders,
                                                   int maxFramePayloadLength, boolean handleCloseFrames) {
        this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength,
             handleCloseFrames, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                          boolean allowExtensions, HttpHeaders customHeaders, int maxFramePayloadLength,
                                          boolean handleCloseFrames, long handshakeTimeoutMillis) {
        this(webSocketURL, version, subprotocol, allowExtensions, customHeaders, maxFramePayloadLength,
             handleCloseFrames, DEFAULT_PERFORM_MASKING, DEFAULT_ALLOW_MASK_MISMATCH, handshakeTimeoutMillis);
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                          boolean allowExtensions, HttpHeaders customHeaders,
                                          int maxFramePayloadLength) {
        this(webSocketURL, version, subprotocol, allowExtensions,
             customHeaders, maxFramePayloadLength, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param webSocketURL
     *            URL for web socket communications. e.g "ws://myhost.com/mypath". Subsequent web socket frames will be
     *            sent to this URL.
     * @param version
     *            Version of web socket specification to use to connect to the server
     * @param subprotocol
     *            Sub protocol request sent to the server.
     * @param customHeaders
     *            Map of custom headers to add to the client request
     * @param maxFramePayloadLength
     *            Maximum length of a frame's payload
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(URI webSocketURL, WebSocketVersion version, String subprotocol,
                                          boolean allowExtensions, HttpHeaders customHeaders,
                                          int maxFramePayloadLength, long handshakeTimeoutMillis) {
        this(webSocketURL, version, subprotocol, allowExtensions, customHeaders,
             maxFramePayloadLength, DEFAULT_HANDLE_CLOSE_FRAMES, handshakeTimeoutMillis);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames) {
        this(handshaker, handleCloseFrames, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames,
                                          long handshakeTimeoutMillis) {
        this(handshaker, handleCloseFrames, DEFAULT_DROP_PONG_FRAMES, handshakeTimeoutMillis);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param dropPongFrames
     *            {@code true} if pong frames should not be forwarded
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames,
                                          boolean dropPongFrames) {
        this(handshaker, handleCloseFrames, dropPongFrames, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param dropPongFrames
     *            {@code true} if pong frames should not be forwarded
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames,
                                          boolean dropPongFrames, long handshakeTimeoutMillis) {
        this(handshaker, handleCloseFrames, dropPongFrames, handshakeTimeoutMillis, DEFAULT_WITH_UTF8_VALIDATOR);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handleCloseFrames
     *            {@code true} if close frames should not be forwarded and just close the channel
     * @param dropPongFrames
     *            {@code true} if pong frames should not be forwarded
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger user
     *            event {@link WebSocketClientHandshakeCompletionEvent}
     * @param withUTF8Validator
     *            {@code true} if UTF8 validation of text frames should be enabled
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, boolean handleCloseFrames,
                                          boolean dropPongFrames, long handshakeTimeoutMillis,
                                          boolean withUTF8Validator) {
        super(dropPongFrames);
        this.handshaker = handshaker;
        this.clientConfig = WebSocketClientProtocolConfig.newBuilder()
            .handleCloseFrames(handleCloseFrames)
            .handshakeTimeoutMillis(handshakeTimeoutMillis)
            .withUTF8Validator(withUTF8Validator)
            .build();
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker) {
        this(handshaker, DEFAULT_HANDSHAKE_TIMEOUT_MILLIS);
    }

    /**
     * Base constructor
     *
     * @param handshaker
     *            The {@link WebSocketClientHandshaker} which will be used to issue the handshake once the connection
     *            was established to the remote peer.
     * @param handshakeTimeoutMillis
     *            Handshake timeout in mills, when handshake timeout, will trigger an inbound channel
     *            event {@link WebSocketClientHandshakeCompletionEvent} with a
     *            {@link WebSocketHandshakeTimeoutException}.
     */
    public WebSocketClientProtocolHandler(WebSocketClientHandshaker handshaker, long handshakeTimeoutMillis) {
        this(handshaker, DEFAULT_HANDLE_CLOSE_FRAMES, handshakeTimeoutMillis);
    }

    @Override
    protected void decodeAndClose(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception {
        if (clientConfig.handleCloseFrames() && frame instanceof CloseWebSocketFrame) {
            Resource.dispose(frame);
            ctx.close();
            return;
        }
        super.decodeAndClose(ctx, frame);
    }

    @Override
    protected WebSocketClientHandshakeException buildHandshakeException(String message) {
        return new WebSocketClientHandshakeException(message);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelPipeline cp = ctx.pipeline();
        if (cp.get(WebSocketClientProtocolHandshakeHandler.class) == null) {
            // Add the WebSocketClientProtocolHandshakeHandler before this one.
            ctx.pipeline().addBefore(ctx.name(), WebSocketClientProtocolHandshakeHandler.class.getName(),
                new WebSocketClientProtocolHandshakeHandler(handshaker, clientConfig.handshakeTimeoutMillis()));
        }
        if (clientConfig.withUTF8Validator() && cp.get(Utf8FrameValidator.class) == null) {
            // Add the UFT8 checking before this one.
            ctx.pipeline().addBefore(ctx.name(), Utf8FrameValidator.class.getName(),
                    new Utf8FrameValidator());
        }
    }
}
