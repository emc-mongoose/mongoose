package com.emc.mongoose.common.conf;
//
import org.apache.commons.configuration.Configuration;
//
import java.io.Externalizable;
/**
 Created by kurila on 20.01.16.
 */
public interface AppConfig
extends Cloneable, Configuration, Externalizable {

	String CONFIG_ROOT = "config.";

	String KEY_AUTH_ID = "auth.id";
	String KEY_AUTH_SECRET = "auth.secret";
	String KEY_AUTH_TOKEN = "auth.token";
	String KEY_IO_BUFFER_SIZE_MIN = "io.buffer.size.min";
	String KEY_IO_BUFFER_SIZE_MAX = "io.buffer.size.max";
	String KEY_ITEM_CLASS = "item.class";
	String KEY_ITEM_CONTAINER_NAME = "item.container.name";
	String KEY_ITEM_DATA_CONTENT_CLASS = "item.data.content.class";
	String KEY_ITEM_DATA_CONTENT_FILE = "item.data.content.file";
	String KEY_ITEM_DATA_CONTENT_SEED = "item.data.content.seed";
	String KEY_ITEM_DATA_CONTENT_SIZE = "item.data.content.size";
	String KEY_ITEM_DATA_RANGES_CLASS = "item.data.ranges.class";
	String KEY_ITEM_DATA_RANGES_FIXED_BYTES = "item.data.ranges.fixed.bytes";
	String KEY_ITEM_DATA_RANGES_RANDOM_COUNT = "item.data.ranges.random.count";
	String KEY_ITEM_DATA_SIZE_CLASS = "item.data.size.class";
	String KEY_ITEM_DATA_SIZE_FIXED = "item.data.size.fixed";
	String KEY_ITEM_DATA_SIZE_RANDOM_BIAS = "item.data.size.random.bias";
	String KEY_ITEM_DATA_SIZE_RANDOM_MAX = "item.data.size.random.max";
	String KEY_ITEM_DATA_SIZE_RANDOM_MIN = "item.data.size.random.min";
	String KEY_ITEM_DATA_VERIFY = "item.data.verify";
	String KEY_ITEM_INPUT_FILE = "item.input.file";
	String KEY_ITEM_INPUT_BATCH_SIZE = "item.input.batchSize";
	String KEY_ITEM_NAMING = "item.naming";
	String KEY_ITEM_QUEUE_SIZE_LIMIT = "item.queue.sizeLimit";
	String KEY_LOAD_CIRCULAR = "load.circular";
	String KEY_LOAD_CLASS = "load.class";
	String KEY_LOAD_THREADS = "load.threads";
	String KEY_LOAD_LIMIT_COUNT = "load.limit.count";
	String KEY_LOAD_LIMIT_RATE = "load.limit.rate";
	String KEY_LOAD_LIMIT_TIME = "load.limit.time";
	String KEY_LOAD_METRICS_PERIOD = "load.metricsPeriod";
	String KEY_LOAD_SERVER_ADDRS = "load.server.addrs";
	String KEY_LOAD_SERVER_ASSIGN_TO_NODE = "load.server.assignTo.node";
	String KEY_RUN_ID = "run.id";
	String KEY_RUN_MODE = "run.mode";
	String KEY_RUN_NAME = "run.name";
	String KEY_RUN_RESUME_ENABLED = "run.resume.enabled";
	String KEY_RUN_VERSION = "run.version";
	String KEY_STORAGE_CLASS = "storage.class";
	String KEY_STORAGE_HTTP_ADDRS = "storage.http.addrs";
	String KEY_STORAGE_HTTP_API_CLASS = "storage.http.api.class";
	String KEY_STORAGE_HTTP_API___PORT = "storage.http.api.%s.port";
	String KEY_STORAGE_HTTP_FS_ACCESS = "storage.http.fsAccess";
	String KEY_STORAGE_HTTP_HEADERS = "storage.http.headers";
	String KEY_STORAGE_HTTP_NAMESPACE = "storage.http.namespace";
	String KEY_STORAGE_HTTP_VERSIONING = "storage.http.versioning";
	String KEY_STORAGE_HTTP_MOCK_HEAD_COUNT = "storage.http.mock.headCount";
	String KEY_STORAGE_HTTP_MOCK_WORKERS_PER_SOCKET = "storage.http.mock.workersPerSocket";
	String KEY_STORAGE_HTTP_MOCK_CAPACITY = "storage.http.mock.capacity";
	String KEY_STORAGE_HTTP_MOCK_CONTAINER_CAPACITY = "storage.http.mock.container.capacity";
	String KEY_STORAGE_HTTP_MOCK_CONTAINER_COUNT_LIMIT = "storage.http.mock.container.countLimit";
	String KEY_NETWORK_SERVE_JMX = "network.serveJMX";
	String KEY_NETWORK_SOCKET_TIMEOUT_MILLISEC = "network.socket.timeoutMilliSec";
	String KEY_NETWORK_SOCKET_REUSE_ADDR = "network.socket.reuseAddr";
	String KEY_NETWORK_SOCKET_KEEP_ALIVE = "network.socket.keepAlive";
	String KEY_NETWORK_SOCKET_TCP_NO_DELAY = "network.socket.tcpNoDelay";
	String KEY_NETWORK_SOCKET_LINGER = "network.socket.linger";
	String KEY_NETWORK_SOCKET_BIND_BACKLOG_SIZe = "network.socket.bindBacklogSize";
	String KEY_NETWORK_SOCKET_INTEREST_OP_QUEUED = "network.socket.interestOpQueued";
	String KEY_NETWORK_SOCKET_SELECT_INTERVAL = "network.socket.selectInterval";
	String FNAME_CONF = CONFIG_ROOT + "json";
	String PREFIX_KEY_ALIASING = "aliasing";

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getAuthId();

	String getAuthSecret();

	String getAuthToken();

	////////////////////////////////////////////////////////////////////////////////////////////////

	int getIoBufferSizeMin();

	int getIoBufferSizeMax();

	////////////////////////////////////////////////////////////////////////////////////////////////

	enum ItemImpl { CONTAINER, DATA }
	ItemImpl getItemClass();

	String getItemContainerName();

	enum ContentSourceImpl { FILE, SEED }
	ContentSourceImpl getItemDataContentClass();

	String getItemDataContentFile();

	String getItemDataContentSeed();

	int getItemDataContentSize();

	enum DataRangesScheme { FIXED, RANDOM }
	DataRangesScheme getItemDataRangesClass();

	String getItemDataRangesFixedBytes();

	int getItemDataContentRangesRandomCount();

	enum DataSizeScheme { FIXED, RANDOM }
	DataSizeScheme getItemDataSizeClass();

	long getItemDataSizeFixed();

	long getItemDataSizeRandomMin();

	long getItemDataSizeRandomMax();

	double getItemDataSizeRandomBias();

	boolean getItemDataVerify();

	String getItemInputFile();

	int getItemInputBatchSize();

	ItemNamingScheme getItemNaming();

	int getItemQueueSizeLimit();

	////////////////////////////////////////////////////////////////////////////////////////////////

	boolean getLoadCircular();

	enum LoadType { WRITE, READ, DELETE }
	LoadType getLoadClass();

	int getLoadThreads();

	long getLoadLimitCount();

	double getLoadLimitRate();

	/** Return the time limit converted into the seconds */
	long getLoadLimitTime();

	/** Return the period in seconds */
	int getLoadMetricsPeriod();

	String[] getLoadServerAddrs();

	boolean getLoadServerAssignToNode();

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

	////////////////////////////////////////////////////////////////////////////////////////////////

	String getRunId();

	String getRunMode();

	String getRunName();

	boolean getRunResumeEnabled();

	String getRunVersion();

	////////////////////////////////////////////////////////////////////////////////////////////////

	enum StorageType { FS, HTTP }
	StorageType getStorageClass();

	String[] getStorageHttpAddrs();
	String[] getStorageHttpAddrsWithPorts();

	String getStorageHttpApiClass();

	int getStorageHttpApi_Port();

	boolean getStroageHttpFsAccess();

	Configuration getStorageHttpHeaders();

	String getStorageHttpNamespace();

	boolean getStorageHttpVersioning();

	int getStorageHttpMockHeadCount();

	int getStorageHttpMockWorkersPerSocket();

	int getStorageHttpMockCapacity();

	int getStorageHttpMockContainerCapacity();

	int getStorageHttpMockContainerCountLimit();

	////////////////////////////////////////////////////////////////////////////////////////////////

	String toFormattedString();

	Object clone()
	throws CloneNotSupportedException;
}
