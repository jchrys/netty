/*
 * Copyright 2018 The Netty Project
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

import io.netty5.util.internal.SocketUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Hashtable;
import java.util.List;

final class DirContextUtils {
    private static final Logger logger = LoggerFactory.getLogger(DirContextUtils.class);

    private DirContextUtils() { }

    static void addNameServers(List<InetSocketAddress> defaultNameServers, int defaultPort) {
        // Using jndi-dns to obtain the default name servers.
        //
        // See:
        // - https://docs.oracle.com/javase/8/docs/technotes/guides/jndi/jndi-dns.html
        // - https://mail.openjdk.java.net/pipermail/net-dev/2017-March/010695.html
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put("java.naming.provider.url", "dns://");

        try {
            DirContext ctx = new InitialDirContext(env);
            String dnsUrls = (String) ctx.getEnvironment().get("java.naming.provider.url");
            // Only try if not empty as otherwise we will produce an exception
            if (dnsUrls != null && !dnsUrls.isEmpty()) {
                String[] servers = dnsUrls.split(" ");
                for (String server : servers) {
                    try {
                        URI uri = new URI(server);
                        String host = uri.getHost();
                        if (host == null || host.isEmpty()) {
                            logger.debug(
                                    "Skipping a nameserver URI as host portion could not be extracted: {}", server);
                            // If the host portion can not be parsed we should just skip this entry.
                            continue;
                        }
                        int port  = uri.getPort();
                        defaultNameServers.add(SocketUtils.socketAddress(uri.getHost(), port == -1 ?
                                defaultPort : port));
                    } catch (URISyntaxException e) {
                        logger.debug("Skipping a malformed nameserver URI: {}", server, e);
                    }
                }
            }
        } catch (NamingException ignore) {
            // Will try reflection if this fails.
        }
    }
}
