package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.conf.enums.ItemType;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.conf.enums.StorageType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.configuration.Configuration;
//
import java.io.Externalizable;
import java.util.Map;
/**
 Created by kurila on 20.01.16.
 */
public interface AppConfig
extends Cloneable, Configuration, Externalizable {

	long serialVersionUID = 42L;

	String CONFIG_ROOT = "config";

	String KEY_AUTH_ID = "auth.id";
	String KEY_AUTH_SECRET = "auth.secret";
	String KEY_AUTH_TOKEN = "auth.token";
	String KEY_IO_BUFFER_SIZE_MIN = "io.buffer.size.min";
	String KEY_IO_BUFFER_SIZE_MAX = "io.buffer.size.max";
	String KEY_ITEM_TYPE = "item.type";
	String KEY_ITEM_DATA_CONTENT_FILE = "item.data.content.file";
	String KEY_ITEM_DATA_CONTENT_SEED = "item.data.content.seed";
	String KEY_ITEM_DATA_CONTENT_RING_SIZE = "item.data.content.ringSize";
	String KEY_ITEM_DATA_RANGES = "item.data.ranges";
	String KEY_ITEM_DATA_SIZE = "item.data.size";
	String KEY_ITEM_DATA_VERIFY = "item.data.verify";
	String KEY_ITEM_DST_CONTAINER = "item.dst.container";
	String KEY_ITEM_DST_FILE = "item.dst.file";
	String KEY_ITEM_SRC_CONTAINER = "item.src.container";
	String KEY_ITEM_SRC_FILE = "item.src.file";
	String KEY_ITEM_SRC_BATCH_SIZE = "item.src.batchSize";
	String KEY_ITEM_NAMING_TYPE = "item.naming.type";
	String KEY_ITEM_NAMING_PREFIX = "item.naming.prefix";
	String KEY_ITEM_NAMING_RADIX = "item.naming.radix";
	String KEY_ITEM_NAMING_OFFSET = "item.naming.offset";
	String KEY_ITEM_NAMING_LENGTH = "item.naming.length";
	String KEY_ITEM_QUEUE_SIZE_LIMIT = "item.queue.sizeLimit";
	String KEY_LOAD_CIRCULAR = "load.circular";
	String KEY_LOAD_LIMIT_COUNT = "load.limit.count";
	String KEY_LOAD_LIMIT_RATE = "load.limit.rate";
	String KEY_LOAD_LIMIT_SIZE = "load.limit.size";
	String KEY_LOAD_LIMIT_TIME = "load.limit.time";
	String KEY_LOAD_METRICS_PERIOD = "load.metrics.period";
	String KEY_LOAD_METRICS_PRECONDITION = "load.metrics.precondition";
	String KEY_LOAD_SERVER_ADDRS = "load.server.addrs";
	String KEY_LOAD_SERVER_NODE_MAPPING = "load.server.nodeMapping";
	String KEY_LOAD_THREADS = "load.threads";
	String KEY_LOAD_TYPE = "load.type";
	String KEY_NETWORK_SERVE_JMX = "network.serveJMX";
	String KEY_NETWORK_SOCKET_TIMEOUT_MILLISEC = "network.socket.timeoutMilliSec";
	String KEY_NETWORK_SOCKET_REUSE_ADDR = "network.socket.reuseAddr";
	String KEY_NETWORK_SOCKET_KEEP_ALIVE = "network.socket.keepAlive";
	String KEY_NETWORK_SOCKET_TCP_NO_DELAY = "network.socket.tcpNoDelay";
	String KEY_NETWORK_SOCKET_LINGER = "network.socket.linger";
	String KEY_NETWORK_SOCKET_BIND_BACKLOG_SIZe = "network.socket.bindBacklogSize";
	String KEY_NETWORK_SOCKET_INTEREST_OP_QUEUED = "network.socket.interestOpQueued";
	String KEY_NETWORK_SOCKET_SELECT_INTERVAL = "network.socket.selectInterval";
	String KEY_NETWORK_SSL = "network.ssl";
	String KEY_RUN_ID = "run.id";
	String KEY_RUN_MODE = "run.mode";
	String KEY_RUN_NAME = "run.name";
	String KEY_RUN_VERSION = "run.version";
	String KEY_RUN_FILE = "run.file";
	String KEY_RUN_RESUME_ENABLED = "run.resume.enabled";
	String KEY_STORAGE_TYPE = "storage.type";
	String KEY_STORAGE_ADDRS = "storage.addrs";
	String KEY_STORAGE_PORT = "storage.port";
	String KEY_STORAGE_HTTP_API = "storage.http.api";
	String KEY_STORAGE_HTTP_FS_ACCESS = "storage.http.fsAccess";
	String KEY_STORAGE_HTTP_HEADERS = "storage.http.headers";
	String KEY_STORAGE_HTTP_NAMESPACE = "storage.http.namespace";
	String KEY_STORAGE_HTTP_VERSIONING = "storage.http.versioning";
	String KEY_STORAGE_MOCK_HEAD_COUNT = "storage.mock.headCount";
	String KEY_STORAGE_MOCK_CAPACITY = "storage.mock.capacity";
	String KEY_STORAGE_MOCK_CONTAINER_CAPACITY = "storage.mock.container.capacity";
	String KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT = "storage.mock.container.countLimit";
	//
	String KEY_SCENARIO_FROM_STDIN = "scenarioFromStdIn";
	String KEY_SCENARIO_FROM_WEBUI = "scenarioFromWebUi";
	//
	String FNAME_CONF = "defaults.json";
	String PREFIX_KEY_ALIASING = "aliasing";

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getAuthId();

	String getAuthSecret();

	String getAuthToken();

	////////////////////////////////////////////////////////////////////////////////////////////////

	int getIoBufferSizeMin();

	int getIoBufferSizeMax();

	////////////////////////////////////////////////////////////////////////////////////////////////
	ItemType getItemType();

	String getItemDataContentFile();

	String getItemDataContentSeed();

	long getItemDataContentRingSize();

	DataRangesConfig getItemDataRanges()
	throws DataRangesConfig.InvalidRangeException;

	SizeInBytes getItemDataSize();

	boolean getItemDataVerify();

	String getItemDstContainer();

	String getItemDstFile();

	String getItemSrcContainer();

	String getItemSrcFile();

	int getItemSrcBatchSize();
	ItemNamingType getItemNamingType();

	String getItemNamingPrefix();

	int getItemNamingRadix();

	long getItemNamingOffset();

	int getItemNamingLength();

	int getItemQueueSizeLimit();

	////////////////////////////////////////////////////////////////////////////////////////////////

	boolean getLoadCircular();

	long getLoadLimitCount();

	double getLoadLimitRate();

	long getLoadLimitSize();

	/** Return the time limit converted into the seconds */
	long getLoadLimitTime();

	/** Return the period in seconds */
	int getLoadMetricsPeriod();

	boolean getLoadMetricsPrecondition();

	String[] getLoadServerAddrs();

	boolean getLoadServerNodeMapping();

	int getLoadThreads();

	LoadType getLoadType();

	////////////////////////////////////////////////////////////////////////////////////////////////

	boolean getNetworkServeJmx();

	int getNetworkSocketTimeoutMilliSec();

	boolean getNetworkSocketReuseAddr();

	boolean getNetworkSocketKeepAlive();

	boolean getNetworkSocketTcpNoDelay();

	int getNetworkSocketLinger();

	int getNetworkSocketBindBacklogSize();

	boolean getNetworkSocketInterestOpQueued();

	int getNetworkSocketSelectInterval();

	boolean getNetworkSsl();

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getRunId();

	String getRunMode();

	String getRunName();

	String getRunVersion();

	String getRunFile();

	boolean getRunResumeEnabled();

	////////////////////////////////////////////////////////////////////////////////////////////////
	StorageType getStorageType();

	String[] getStorageAddrs();
	String[] getStorageAddrsWithPorts();

	int getStoragePort();

	String getStorageHttpApi();

	boolean getStorageHttpFsAccess();

	Configuration getStorageHttpHeaders();

	String getStorageHttpNamespace();

	boolean getStorageHttpVersioning();

	int getStorageMockHeadCount();

	int getStorageMockCapacity();

	int getStorageMockContainerCapacity();

	int getStorageMockContainerCountLimit();

	void setRunId(final String runId);

	void setRunMode(final String runMode);

	////////////////////////////////////////////////////////////////////////////////////////////////

	void override(final String branch, final Map<String, ?> tree);

	void findAndSubstitute(final String replacePattern, final Object newValue);

	ObjectNode toJsonTree(final ObjectMapper mapper)
	throws IllegalStateException;

	String toFormattedString();

	Object clone()
	throws CloneNotSupportedException;
}
