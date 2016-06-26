/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package com.emc.mongoose.common.net.http.conn.pool.experimental;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpHost;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.config.ConnectionConfig;
import org.apache.http.impl.nio.pool.BasicNIOConnFactory;
import org.apache.http.nio.NHttpClientConnection;
import org.apache.http.nio.pool.NIOConnFactory;
import org.apache.http.nio.pool.SocketAddressResolver;
import org.apache.http.nio.reactor.ConnectingIOReactor;

/**
 * A very basic {@link org.apache.http.pool.ConnPool} implementation that
 * represents a pool of non-blocking {@link NHttpClientConnection} connections
 * identified by an {@link HttpHost} instance. Please note this pool
 * implementation does not support complex routes via a proxy cannot
 * differentiate between direct and proxied connections.
 *
 * @see HttpHost
 * @since 4.2
 */
@SuppressWarnings("deprecation")
public class BasicLocklessConnPool
extends AbstractLocklessConnPool<HttpHost, NHttpClientConnection, BasicLocklessPoolEntry> {

	private static final AtomicLong COUNTER = new AtomicLong();

	private final int connectTimeout;

	private static class BasicAddressResolver
	implements SocketAddressResolver<HttpHost> {

		@Override
		public final SocketAddress resolveLocalAddress(final HttpHost host) {
			return null;
		}

		@Override
		public final SocketAddress resolveRemoteAddress(final HttpHost host) {
			final String hostname = host.getHostName();
			int port = host.getPort();
			if(port == -1) {
				if (host.getSchemeName().equalsIgnoreCase("http")) {
					port = 80;
				} else if (host.getSchemeName().equalsIgnoreCase("https")) {
					port = 443;
				}
			}
			return new InetSocketAddress(hostname, port);
		}

	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final NIOConnFactory<HttpHost, NHttpClientConnection> connFactory,
		final int connectTimeout
	) {
		super(ioreactor, connFactory, new BasicAddressResolver(), 2, 20);
		this.connectTimeout = connectTimeout;
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final int connectTimeout,
		final ConnectionConfig config) {
		this(ioreactor, new BasicNIOConnFactory(config), connectTimeout);
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(
		final ConnectingIOReactor ioreactor,
		final ConnectionConfig config) {
		this(ioreactor, new BasicNIOConnFactory(config), 0);
	}

	/**
	 * @since 4.3
	 */
	public BasicLocklessConnPool(final ConnectingIOReactor ioreactor) {
		this(ioreactor, new BasicNIOConnFactory(ConnectionConfig.DEFAULT), 0);
	}

	/**
	 * @deprecated (4.3) use {@link SocketAddressResolver}
	 */
	@Deprecated
	@Override
	protected SocketAddress resolveRemoteAddress(final HttpHost host) {
		return new InetSocketAddress(host.getHostName(), host.getPort());
	}

	/**
	 * @deprecated (4.3) use {@link SocketAddressResolver}
	 */
	@Deprecated
	@Override
	protected SocketAddress resolveLocalAddress(final HttpHost host) {
		return null;
	}

	@Override
	protected final BasicLocklessPoolEntry createEntry(
		final HttpHost host, final NHttpClientConnection conn
	) {
		final BasicLocklessPoolEntry entry = new BasicLocklessPoolEntry(
			Long.toString(COUNTER.getAndIncrement()), host, conn
		);
		entry.setSocketTimeout(conn.getSocketTimeout());
		return entry;
	}

	@Override
	public Future<BasicLocklessPoolEntry> lease(
		final HttpHost route,
		final Object state,
		final FutureCallback<BasicLocklessPoolEntry> callback) {
		return super.lease(route, state,
			this.connectTimeout, TimeUnit.MILLISECONDS, callback);
	}

	@Override
	public Future<BasicLocklessPoolEntry> lease(
		final HttpHost route,
		final Object state) {
		return super.lease(route, state, this.connectTimeout, TimeUnit.MILLISECONDS, null);
	}

	@Override
	protected void onLease(final BasicLocklessPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		conn.setSocketTimeout(entry.getSocketTimeout());
	}

	@Override
	protected void onRelease(final BasicLocklessPoolEntry entry) {
		final NHttpClientConnection conn = entry.getConnection();
		entry.setSocketTimeout(conn.getSocketTimeout());
		conn.setSocketTimeout(0);
	}

}
