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

import io.netty5.channel.Channel;
import io.netty5.channel.EventLoop;
import io.netty5.handler.codec.dns.DnsQuestion;
import io.netty5.handler.codec.dns.DnsRecord;
import io.netty5.handler.codec.dns.DnsRecordType;
import io.netty5.util.concurrent.Future;
import io.netty5.util.concurrent.Promise;

import java.net.UnknownHostException;
import java.util.List;

final class DnsRecordResolveContext extends DnsResolveContext<DnsRecord> {

    DnsRecordResolveContext(DnsNameResolver parent, Channel channel, Future<? extends Channel> channelReadyFuture,
                            Promise<?> originalPromise, DnsQuestion question, DnsRecord[] additionals,
                            DnsServerAddressStream nameServerAddrs, int allowedQueries) {
        this(parent, channel, channelReadyFuture, originalPromise, question.name(), question.dnsClass(),
             new DnsRecordType[] { question.type() },
             additionals, nameServerAddrs, allowedQueries);
    }

    private DnsRecordResolveContext(DnsNameResolver parent, Channel channel,
                                    Future<? extends Channel> channelReadyFuture, Promise<?> originalPromise,
                                    String hostname, int dnsClass, DnsRecordType[] expectedTypes,
                                    DnsRecord[] additionals,
                                    DnsServerAddressStream nameServerAddrs,
                                    int allowedQueries) {
        super(parent, channel, channelReadyFuture, originalPromise, hostname, dnsClass, expectedTypes,
                additionals, nameServerAddrs, allowedQueries);
    }

    @Override
    DnsResolveContext<DnsRecord> newResolverContext(DnsNameResolver parent, Channel channel,
                                                    Future<? extends Channel> channelReadyFuture,
                                                    Promise<?> originalPromise,
                                                    String hostname,
                                                    int dnsClass, DnsRecordType[] expectedTypes,
                                                    DnsRecord[] additionals,
                                                    DnsServerAddressStream nameServerAddrs,
                                                    int allowedQueries) {
        return new DnsRecordResolveContext(parent, channel, channelReadyFuture, originalPromise, hostname, dnsClass,
                                           expectedTypes, additionals, nameServerAddrs, allowedQueries);
    }

    @Override
    DnsRecord convertRecord(DnsRecord record, String hostname, DnsRecord[] additionals, EventLoop eventLoop) {
        // The given record is a shared object, but we need to give back a record that has its own life time.
        return record.copy();
    }

    @Override
    List<DnsRecord> filterResults(List<DnsRecord> unfiltered) {
        return unfiltered;
    }

    @Override
    boolean isCompleteEarly(DnsRecord resolved) {
        return false;
    }

    @Override
    boolean isDuplicateAllowed() {
        return true;
    }

    @Override
    void cache(String hostname, DnsRecord[] additionals, DnsRecord result, DnsRecord convertedResult) {
        // Do not cache.
        // XXX: When we implement cache, we would need to retain the reference count of the result record.
    }

    @Override
    void cache(String hostname, DnsRecord[] additionals, UnknownHostException cause) {
        // Do not cache.
        // XXX: When we implement cache, we would need to retain the reference count of the result record.
    }

    @Override
    DnsCnameCache cnameCache() {
        // We don't use a cache here at all as we also don't cache if we end up using the DnsRecordResolverContext.
        return NoopDnsCnameCache.INSTANCE;
    }
}
