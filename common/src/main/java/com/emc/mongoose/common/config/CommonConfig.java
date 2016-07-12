package com.emc.mongoose.common.config;

/**
 Created on 11.07.16.
 */
public class CommonConfig {

	public static final String KEY_NAME = "name";
	public static final String KEY_NETWORK = "network";
	private final String name;
	private final NetworkConfig networkConfig;

	public CommonConfig(final String name, final NetworkConfig networkConfig) {
		this.name = name;
		this.networkConfig = networkConfig;
	}

	public String getName() {
		return name;
	}

	public NetworkConfig getNetworkConfig() {
		return networkConfig;
	}

	public static class NetworkConfig {

		public static final String KEY_SOCKET = "socket";
		private final SocketConfig socketConfig;

		public NetworkConfig(final SocketConfig socketConfig) {
			this.socketConfig = socketConfig;
		}

		public SocketConfig getSocketConfig() {
			return socketConfig;
		}

		public static class SocketConfig {
			public static final String KEY_TIMEOUT_IN_MILLISECONDS = "timeoutMilliSec";
			public static final String KEY_REUSABLE_ADDRESS = "reuseAddr";
			public static final String KEY_KEEP_ALIVE = "keepAlive";
			public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
			public static final String KEY_LINGER = "linger";
			public static final String KEY_BIND_BACK_LOG_SIZE = "bindBacklogSize";
			public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
			public static final String KEY_SELECT_INTERVAL = "selectInterval";
			private final int timeoutMilliSec;
			private final boolean reuseAddr;
			private final boolean keepAlive;
			private final boolean tcpNoDelay;
			private final int linger;
			private final int bindBackLogSize;
			private final boolean interestOpQueued;
			private final int selectInterval;

			public SocketConfig(
				final int timeoutMilliSec, final boolean reuseAddr, final boolean keepAlive,
				final boolean tcpNoDelay, final int linger, final int bindBackLogSize,
				final boolean interestOpQueued, final int selectInterval
			) {
				this.timeoutMilliSec = timeoutMilliSec;
				this.reuseAddr = reuseAddr;
				this.keepAlive = keepAlive;
				this.tcpNoDelay = tcpNoDelay;
				this.linger = linger;
				this.bindBackLogSize = bindBackLogSize;
				this.interestOpQueued = interestOpQueued;
				this.selectInterval = selectInterval;
			}

			public int getTimeoutInMilliseconds() {
				return timeoutMilliSec;
			}

			public boolean getReusableAddress() {
				return reuseAddr;
			}

			public boolean getKeepAlive() {
				return keepAlive;
			}

			public boolean getTcpNoDelay() {
				return tcpNoDelay;
			}

			public int getLinger() {
				return linger;
			}

			public int getBindBackLogSize() {
				return bindBackLogSize;
			}

			public boolean getInterestOpQueued() {
				return interestOpQueued;
			}

			public int getSelectInterval() {
				return selectInterval;
			}
		}

	}

}
