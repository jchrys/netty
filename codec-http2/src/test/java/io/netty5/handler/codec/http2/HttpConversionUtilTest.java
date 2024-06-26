/*
 * Copyright 2017 The Netty Project
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
package io.netty5.handler.codec.http2;

import io.netty5.handler.codec.http.DefaultHttpRequest;
import io.netty5.handler.codec.http.HttpMethod;
import io.netty5.handler.codec.http.HttpRequest;
import io.netty5.handler.codec.http.HttpVersion;
import io.netty5.handler.codec.http.headers.DefaultHttpHeadersFactory;
import io.netty5.handler.codec.http.headers.HeaderValidationException;
import io.netty5.handler.codec.http.headers.HttpHeaders;
import io.netty5.handler.codec.http2.headers.Http2Headers;
import io.netty5.util.AsciiString;
import org.junit.jupiter.api.Test;

import static io.netty5.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty5.handler.codec.http.HttpHeaderNames.COOKIE;
import static io.netty5.handler.codec.http.HttpHeaderNames.HOST;
import static io.netty5.handler.codec.http.HttpHeaderNames.KEEP_ALIVE;
import static io.netty5.handler.codec.http.HttpHeaderNames.PROXY_CONNECTION;
import static io.netty5.handler.codec.http.HttpHeaderNames.TE;
import static io.netty5.handler.codec.http.HttpHeaderNames.TRANSFER_ENCODING;
import static io.netty5.handler.codec.http.HttpHeaderNames.UPGRADE;
import static io.netty5.handler.codec.http.HttpHeaderValues.GZIP;
import static io.netty5.handler.codec.http.HttpHeaderValues.TRAILERS;
import static io.netty5.util.AsciiString.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HttpConversionUtilTest {

    @Test
    public void connectNoPath() throws Exception {
        String authority = "netty.io:80";
        Http2Headers headers = Http2Headers.newHeaders();
        headers.authority(authority);
        headers.method(HttpMethod.CONNECT.asciiName());
        HttpRequest request = HttpConversionUtil.toHttpRequest(0, headers, DefaultHttpHeadersFactory.headersFactory());
        assertNotNull(request);
        assertEquals(authority, request.uri());
        assertEquals(of(authority), request.headers().get(HOST));
    }

    @Test
    public void setHttp2AuthorityWithoutUserInfo() {
        Http2Headers headers = Http2Headers.newHeaders();

        HttpConversionUtil.setHttp2Authority("foo", headers);
        assertEquals(new AsciiString("foo"), headers.authority());
    }

    @Test
    public void setHttp2AuthorityWithUserInfo() {
        Http2Headers headers = Http2Headers.newHeaders();

        HttpConversionUtil.setHttp2Authority("info@foo", headers);
        assertEquals(new AsciiString("foo"), headers.authority());

        HttpConversionUtil.setHttp2Authority("@foo.bar", headers);
        assertEquals(new AsciiString("foo.bar"), headers.authority());
    }

    @Test
    public void setHttp2AuthorityNullOrEmpty() {
        Http2Headers headers = Http2Headers.newHeaders();

        HttpConversionUtil.setHttp2Authority(null, headers);
        assertNull(headers.authority());

        // https://datatracker.ietf.org/doc/html/rfc9113#section-8.3.1
        // Clients that generate HTTP/2 requests directly MUST use the ":authority" pseudo-header
        // field to convey authority information, unless there is no authority information to convey
        // (in which case it MUST NOT generate ":authority").
        // An intermediary that forwards a request over HTTP/2 MUST construct an ":authority" pseudo-header
        // field using the authority information from the control data of the original request, unless the
        // original request's target URI does not contain authority information
        // (in which case it MUST NOT generate ":authority").
        assertThrows(HeaderValidationException.class,
                () -> HttpConversionUtil.setHttp2Authority("", Http2Headers.newHeaders()));
    }

    @Test
    public void setHttp2AuthorityWithEmptyAuthority() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpConversionUtil.setHttp2Authority("info@", Http2Headers.newHeaders()));
    }

    @Test
    public void stripTEHeaders() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(TE, GZIP);
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertTrue(out.isEmpty());
    }

    @Test
    public void stripTEHeadersExcludingTrailers() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(TE, GZIP);
        inHeaders.add(TE, TRAILERS);
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertSame(TRAILERS, out.get(TE));
    }

    @Test
    public void stripTEHeadersCsvSeparatedExcludingTrailers() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(TE, GZIP + "," + TRAILERS);
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertSame(TRAILERS, out.get(TE));
    }

    @Test
    public void stripTEHeadersCsvSeparatedAccountsForValueSimilarToTrailers() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(TE, GZIP + "," + TRAILERS + "foo");
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertFalse(out.contains(TE));
    }

    @Test
    public void stripTEHeadersAccountsForValueSimilarToTrailers() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(TE, TRAILERS + "foo");
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertFalse(out.contains(TE));
    }

    @Test
    public void stripTEHeadersAccountsForOWS() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders(false);
        inHeaders.add(TE, " " + TRAILERS + ' ');
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertSame(TRAILERS, out.get(TE));
    }

    @Test
    public void stripConnectionHeadersAndNominees() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(CONNECTION, "foo");
        inHeaders.add("foo", "bar");
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertTrue(out.isEmpty());
    }

    @Test
    public void stripConnectionNomineesWithCsv() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(CONNECTION, "foo,  bar");
        inHeaders.add("foo", "baz");
        inHeaders.add("bar", "qux");
        inHeaders.add("hello", "world");
        Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertEquals(1, out.size());
        assertSame("world", out.get("hello"));
    }

    @Test
    public void cookieNoSpace() {
        final HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(COOKIE, "one=foo;two=bar");
        final Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertEquals("one=foo;two=bar", out.get(COOKIE)); // not split
    }

    @Test
    public void cookieTailSemicolon() {
        final HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(COOKIE, "one=foo;");
        final Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertEquals("one=foo;", out.get(COOKIE)); // not split
    }

    @Test
    public void cookieNonAscii() {
        final HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(COOKIE, "one=\uD83D\uDE43; two=ü");
        final Http2Headers out = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, out);
        assertSame("one=\uD83D\uDE43; two=ü", out.get(COOKIE)); // not split
    }

    @Test
    public void handlesRequest() throws Exception {
        HttpRequest msg = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "http://example.com/path/to/something");
        HttpHeaders inHeaders = msg.headers();
        inHeaders.add(CONNECTION, "foo,  bar");
        inHeaders.add("hello", "world");
        Http2Headers out = HttpConversionUtil.toHttp2Headers(msg, true, true, true);
        assertEquals(new AsciiString("/path/to/something"), out.path());
        assertEquals(new AsciiString("http"), out.scheme());
        assertEquals(new AsciiString("example.com"), out.authority());
        assertEquals(HttpMethod.GET.asciiName(), out.method());
        assertEquals("world", out.get("hello"));
    }

    @Test
    public void handlesRequestWithDoubleSlashPath() throws Exception {
        HttpRequest msg = new DefaultHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, "//path/to/something");
        HttpHeaders inHeaders = msg.headers();
        inHeaders.add(CONNECTION, "foo,  bar");
        inHeaders.add(HOST, "example.com");
        inHeaders.add(HttpConversionUtil.ExtensionHeaderNames.SCHEME.text(), "http");
        inHeaders.add("hello", "world");
        Http2Headers out = HttpConversionUtil.toHttp2Headers(msg, true, true, true);
        assertEquals(new AsciiString("//path/to/something"), out.path());
        assertEquals(new AsciiString("http"), out.scheme());
        assertEquals(new AsciiString("example.com"), out.authority());
        assertEquals(HttpMethod.GET.asciiName(), out.method());
    }

    @Test
    public void addHttp2ToHttpHeadersCombinesCookies() throws Http2Exception {
        Http2Headers inHeaders = Http2Headers.newHeaders();
        inHeaders.add("yes", "no");
        inHeaders.add(COOKIE, "foo=bar");
        inHeaders.add(COOKIE, "bax=baz");

        HttpHeaders outHeaders = HttpHeaders.newHeaders();

        HttpConversionUtil.addHttp2ToHttpHeaders(5, inHeaders, outHeaders, HttpVersion.HTTP_1_1, false, false);
        assertEquals("no", outHeaders.get("yes"));
        assertEquals("foo=bar; bax=baz", outHeaders.get(COOKIE.toString()));
    }

    @Test
    public void connectionSpecificHeadersShouldBeRemoved() {
        HttpHeaders inHeaders = HttpHeaders.newHeaders();
        inHeaders.add(CONNECTION, "keep-alive");
        inHeaders.add(HOST, "example.com");
        @SuppressWarnings("deprecation")
        AsciiString keepAlive = KEEP_ALIVE;
        inHeaders.add(keepAlive, "timeout=5, max=1000");
        @SuppressWarnings("deprecation")
        AsciiString proxyConnection = PROXY_CONNECTION;
        inHeaders.add(proxyConnection, "timeout=5, max=1000");
        inHeaders.add(TRANSFER_ENCODING, "chunked");
        inHeaders.add(UPGRADE, "h2c");

        Http2Headers outHeaders = Http2Headers.newHeaders();
        HttpConversionUtil.toHttp2Headers(inHeaders, outHeaders);

        assertFalse(outHeaders.contains(CONNECTION));
        assertFalse(outHeaders.contains(HOST));
        assertFalse(outHeaders.contains(keepAlive));
        assertFalse(outHeaders.contains(proxyConnection));
        assertFalse(outHeaders.contains(TRANSFER_ENCODING));
        assertFalse(outHeaders.contains(UPGRADE));
    }

    @Test
    public void http2ToHttpHeaderTest() throws Exception {
        Http2Headers http2Headers = Http2Headers.newHeaders();
        http2Headers.status("200");
        http2Headers.path("/meow"); // HTTP/2 Header response should not contain 'path' in response.
        http2Headers.set("cat", "meow");

        HttpHeaders httpHeaders = HttpHeaders.newHeaders();
        HttpConversionUtil.addHttp2ToHttpHeaders(3, http2Headers, httpHeaders, HttpVersion.HTTP_1_1, false, true);
        assertFalse(httpHeaders.contains(HttpConversionUtil.ExtensionHeaderNames.PATH.text()));
        assertEquals("meow", httpHeaders.get("cat"));

        httpHeaders.clear();
        HttpConversionUtil.addHttp2ToHttpHeaders(3, http2Headers, httpHeaders, HttpVersion.HTTP_1_1, false, false);
        assertTrue(httpHeaders.contains(HttpConversionUtil.ExtensionHeaderNames.PATH.text()));
        assertEquals("meow", httpHeaders.get("cat"));
    }
}
