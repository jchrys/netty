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
package io.netty5.resolver.dns;

import io.netty5.util.NetUtil;
import io.netty5.util.internal.PlatformDependent;
import io.netty5.util.internal.SocketUtils;
import io.netty5.util.internal.SystemPropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet6Address;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.netty5.resolver.dns.DnsServerAddresses.sequential;

/**
 * A {@link DnsServerAddressStreamProvider} which will use predefined default DNS servers to use for DNS resolution.
 * These defaults do not respect your host's machines defaults.
 * <p>
 * This may use the JDK's blocking DNS resolution to bootstrap the default DNS server addresses.
 */
public final class DefaultDnsServerAddressStreamProvider implements DnsServerAddressStreamProvider {
    private static final Logger logger = LoggerFactory.getLogger(DefaultDnsServerAddressStreamProvider.class);
    private static final String DEFAULT_FALLBACK_SERVER_PROPERTY = "io.netty5.resolver.dns.defaultNameServerFallback";
    public static final DefaultDnsServerAddressStreamProvider INSTANCE = new DefaultDnsServerAddressStreamProvider();

    private static final List<InetSocketAddress> DEFAULT_NAME_SERVER_LIST;
    private static final DnsServerAddresses DEFAULT_NAME_SERVERS;
    static final int DNS_PORT = 53;

    static {
        final List<InetSocketAddress> defaultNameServers = new ArrayList<>(2);
        if (!PlatformDependent.isAndroid()) {
            // Skip this on Android; it has neither /etc/resolv.conf nor JNDI classes.
            // See https://github.com/netty/netty/issues/8654
            if (!PlatformDependent.isWindows()) {
                // Try reading /etc/resolv.conf. It's usually found on Linux or macOS, but can also be missing.
                try {
                    defaultNameServers.addAll(ResolvConf.system().getNameservers());
                } catch (IllegalStateException e) {
                    String fallbackMessage = "Failed to get name servers from /etc/resolv.conf; will fall back to JNDI";
                    if (logger.isDebugEnabled()) {
                        // Always log at INFO, but only include stack trace if DEBUG is enabled.
                        logger.info(fallbackMessage, e);
                    } else {
                        logger.info(fallbackMessage);
                    }
                    DirContextUtils.addNameServers(defaultNameServers, DNS_PORT);
                }
            } else {
                DirContextUtils.addNameServers(defaultNameServers, DNS_PORT);
            }
        }

        if (!defaultNameServers.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug(
                        "Default DNS servers: {} (sun.net.dns.ResolverConfiguration)", defaultNameServers);
            }
        } else {
            String defaultNameserverString = SystemPropertyUtil.get(DEFAULT_FALLBACK_SERVER_PROPERTY, null);
            if (defaultNameserverString != null) {
                for (String server : defaultNameserverString.split(",")) {
                    String dns = server.trim();
                    if (!NetUtil.isValidIpV4Address(dns) && !NetUtil.isValidIpV6Address(dns)) {
                        throw new ExceptionInInitializerError(DEFAULT_FALLBACK_SERVER_PROPERTY + " doesn't" +
                                " contain a valid list of NameServers: " + defaultNameserverString);
                    }
                    defaultNameServers.add(SocketUtils.socketAddress(server.trim(), DNS_PORT));
                }
                if (defaultNameServers.isEmpty()) {
                    throw new ExceptionInInitializerError(DEFAULT_FALLBACK_SERVER_PROPERTY + " doesn't" +
                            " contain a valid list of NameServers: " + defaultNameserverString);
                }

                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Default DNS servers: {} (Configured by {} system property)",
                            defaultNameServers, DEFAULT_FALLBACK_SERVER_PROPERTY);
                }
            } else {
                // Depending if IPv6 or IPv4 is used choose the correct DNS servers provided by google:
                // https://developers.google.com/speed/public-dns/docs/using
                // https://docs.oracle.com/javase/7/docs/api/java/net/doc-files/net-properties.html
                if (NetUtil.isIpV6AddressesPreferred() ||
                        (NetUtil.LOCALHOST instanceof Inet6Address && !NetUtil.isIpV4StackPreferred())) {
                    Collections.addAll(
                            defaultNameServers,
                            SocketUtils.socketAddress("2001:4860:4860::8888", DNS_PORT),
                            SocketUtils.socketAddress("2001:4860:4860::8844", DNS_PORT));
                } else {
                    Collections.addAll(
                            defaultNameServers,
                            SocketUtils.socketAddress("8.8.8.8", DNS_PORT),
                            SocketUtils.socketAddress("8.8.4.4", DNS_PORT));
                }

                if (logger.isWarnEnabled()) {
                    logger.warn(
                            "Default DNS servers: {} (Google Public DNS as a fallback)", defaultNameServers);
                }
            }
        }

        DEFAULT_NAME_SERVER_LIST = Collections.unmodifiableList(defaultNameServers);
        DEFAULT_NAME_SERVERS = sequential(DEFAULT_NAME_SERVER_LIST);
    }

    private DefaultDnsServerAddressStreamProvider() {
    }

    @Override
    public DnsServerAddressStream nameServerAddressStream(String hostname) {
        return DEFAULT_NAME_SERVERS.stream();
    }

    /**
     * Returns the list of the system DNS server addresses. If it failed to retrieve the list of the system DNS server
     * addresses from the environment, it will return {@code "8.8.8.8"} and {@code "8.8.4.4"}, the addresses of the
     * Google public DNS servers.
     */
    public static List<InetSocketAddress> defaultAddressList() {
        return DEFAULT_NAME_SERVER_LIST;
    }

    /**
     * Returns the {@link DnsServerAddresses} that yields the system DNS server addresses sequentially. If it failed to
     * retrieve the list of the system DNS server addresses from the environment, it will use {@code "8.8.8.8"} and
     * {@code "8.8.4.4"}, the addresses of the Google public DNS servers.
     * <p>
     * This method has the same effect with the following code:
     * <pre>
     * DnsServerAddresses.sequential(DnsServerAddresses.defaultAddressList());
     * </pre>
     * </p>
     */
    public static DnsServerAddresses defaultAddresses() {
        return DEFAULT_NAME_SERVERS;
    }
}
