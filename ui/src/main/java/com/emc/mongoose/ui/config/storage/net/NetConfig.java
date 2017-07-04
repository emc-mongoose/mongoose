package com.emc.mongoose.ui.config.storage.net;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.ui.config.SizeInBytesDeserializer;
import com.emc.mongoose.ui.config.SizeInBytesSerializer;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;
import com.emc.mongoose.ui.config.storage.net.node.NodeConfig;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.io.Serializable;

/**
 Created by andrey on 05.07.17.
 */
public final class NetConfig
implements Serializable {

	public static final String KEY_TIMEOUT_MILLI_SEC = "timeoutMilliSec";
	public static final String KEY_REUSE_ADDR = "reuseAddr";
	public static final String KEY_KEEP_ALIVE = "keepAlive";
	public static final String KEY_TCP_NO_DELAY = "tcpNoDelay";
	public static final String KEY_LINGER = "linger";
	public static final String KEY_BIND_BACKLOG_SIZE = "bindBacklogSize";
	public static final String KEY_INTEREST_OP_QUEUED = "interestOpQueued";
	public static final String KEY_RCV_BUF = "rcvBuf";
	public static final String KEY_SND_BUF = "sndBuf";
	public static final String KEY_SSL = "ssl";
	public static final String KEY_HTTP = "http";
	public static final String KEY_NODE = "node";

	public final int getTimeoutMilliSec() {
		return timeoutMilliSec;
	}

	public final boolean getReuseAddr() {
		return reuseAddr;
	}

	public final boolean getKeepAlive() {
		return keepAlive;
	}

	public final boolean getTcpNoDelay() {
		return tcpNoDelay;
	}

	public final int getLinger() {
		return linger;
	}

	public final int getBindBacklogSize() {
		return bindBacklogSize;
	}

	public final boolean getInterestOpQueued() {
		return interestOpQueued;
	}

	public final SizeInBytes getRcvBuf() {
		return rcvBuf;
	}

	public final SizeInBytes getSndBuf() {
		return sndBuf;
	}

	public boolean getSsl() {
		return ssl;
	}

	public HttpConfig getHttpConfig() {
		return httpConfig;
	}

	public NodeConfig getNodeConfig() {
		return nodeConfig;
	}

	public final void setTimeoutMilliSec(final int timeoutMilliSec) {
		this.timeoutMilliSec = timeoutMilliSec;
	}

	public final void setReuseAddr(final boolean reuseAddr) {
		this.reuseAddr = reuseAddr;
	}

	public final void setKeepAlive(final boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public final void setTcpNoDelay(final boolean tcpNoDelay) {
		this.tcpNoDelay = tcpNoDelay;
	}

	public final void setLinger(final int linger) {
		this.linger = linger;
	}

	public final void setBindBacklogSize(final int bindBacklogSize) {
		this.bindBacklogSize = bindBacklogSize;
	}

	public final void setInterestOpQueued(final boolean interestOpQueued) {
		this.interestOpQueued = interestOpQueued;
	}

	public final void setRcvBuf(final SizeInBytes rcvBuf) {
		this.rcvBuf = rcvBuf;
	}

	public final void setSndBuf(final SizeInBytes sndBuf) {
		this.sndBuf = sndBuf;
	}

	public final void setSsl(final boolean ssl) {
		this.ssl = ssl;
	}

	public final void setHttpConfig(final HttpConfig httpConfig) {
		this.httpConfig = httpConfig;
	}

	public final void setNodeConfig(final NodeConfig nodeConfig) {
		this.nodeConfig = nodeConfig;
	}

	@JsonProperty(KEY_TIMEOUT_MILLI_SEC) private int timeoutMilliSec;

	@JsonProperty(KEY_REUSE_ADDR) private boolean reuseAddr;

	@JsonProperty(KEY_KEEP_ALIVE) private boolean keepAlive;

	@JsonProperty(KEY_TCP_NO_DELAY) private boolean tcpNoDelay;

	@JsonProperty(KEY_LINGER) private int linger;

	@JsonProperty(KEY_BIND_BACKLOG_SIZE) private int bindBacklogSize;

	@JsonProperty(KEY_INTEREST_OP_QUEUED) private boolean interestOpQueued;

	@JsonProperty(KEY_RCV_BUF)
	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	private SizeInBytes rcvBuf;

	@JsonProperty(KEY_SND_BUF)
	@JsonDeserialize(using = SizeInBytesDeserializer.class)
	@JsonSerialize(using = SizeInBytesSerializer.class)
	private SizeInBytes sndBuf;

	@JsonProperty(KEY_SSL) private boolean ssl;
	@JsonProperty(KEY_HTTP) private HttpConfig httpConfig;
	@JsonProperty(KEY_NODE) private NodeConfig nodeConfig;

	public NetConfig() {
	}

	public NetConfig(final NetConfig other) {
		this.timeoutMilliSec = other.getTimeoutMilliSec();
		this.reuseAddr = other.getReuseAddr();
		this.keepAlive = other.getKeepAlive();
		this.tcpNoDelay = other.getTcpNoDelay();
		this.linger = other.getLinger();
		this.bindBacklogSize = other.getBindBacklogSize();
		this.interestOpQueued = other.getInterestOpQueued();
		this.rcvBuf = new SizeInBytes(other.getRcvBuf());
		this.sndBuf = new SizeInBytes(other.getSndBuf());
		this.ssl = other.getSsl();
		this.httpConfig = new HttpConfig(other.getHttpConfig());
		this.nodeConfig = new NodeConfig(other.getNodeConfig());
	}
}