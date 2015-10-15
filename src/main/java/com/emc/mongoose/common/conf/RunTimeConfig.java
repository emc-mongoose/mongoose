package com.emc.mongoose.common.conf;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.databind.JsonNode;
//
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
//
import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.nio.file.Paths;

/**
 Created by kurila on 28.05.14.
 A shared runtime configuration.
 */
public final class RunTimeConfig
extends BaseConfiguration
implements Externalizable {
	//
	public final static String
		LIST_SEP = ",",
		STORAGE_PORT_SEP = ":",
		//
		PREFIX_KEY_ALIASING = "aliasing.",
		//
		KEY_AUTH_ID = "auth.id",
		KEY_AUTH_SECRET = "auth.secret",
		//
		KEY_IO_BUFFER_SIZE_MIN = "io.buffer.size.min",
		KEY_IO_BUFFER_SIZE_MAX = "io.buffer.size.max",
		//
		KEY_DATA_SRC_RING_SEED = "data.src.ring.seed",
		KEY_DATA_SRC_RING_SIZE = "data.src.ring.size",
		KEY_DATA_SRC_RING_UNIFORM_SIZE = "data.src.ring.uniformSize",
		//
		KEY_DATA_ITEM_COUNT = "load.limit.count",
		KEY_DATA_SIZE = "data.size",
		KEY_DATA_SIZE_MIN = "data.size.min",
		KEY_DATA_SIZE_MAX = "data.size.max",
		KEY_DATA_SIZE_BIAS = "data.size.bias",
		KEY_DATA_SRC_FPATH = "data.src.fpath",
		KEY_DATA_FS_ACCESS = "data.fsAccess",
		KEY_DATA_PREFIX = "data.prefix",
		KEY_DATA_VERSIONING = "data.versioning",
		KEY_DATA_SRC_CIRCULAR = "data.src.circular",
		KEY_DATA_SRC_RANDOM = "data.src.random",
		KEY_DATA_SRC_BATCH_SIZE = "data.src.batchSize",
		//
		KEY_HTTP_PIPELINING = "http.pipelining",
		//
		KEY_LOAD_CONNS = "load.connections",
		KEY_LOAD_SERVER_ADDRS = "load.server.addrs",
		KEY_LOAD_SERVER_ASSIGN2_NODE = "load.server.assignTo.node",
		KEY_CREATE_CONNS = "load.type.create.connections",
		KEY_READ_CONNS = "load.type.read.connections",
		KEY_UPDATE_CONNS = "load.type.update.connections",
		KEY_DELETE_CONNS = "load.type.delete.connections",
		KEY_APPEND_CONNS = "load.type.append.connections",
		KEY_LOAD_WORKERS = "load.workers",
		KEY_LOAD_UPDATE_PER_ITEM = "load.type.update.perItem",
		//
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode",
		KEY_SCENARIO_NAME = "scenario.name",
		KEY_LOAD_METRICS_PERIOD_SEC = "load.metricsPeriodSec",
		KEY_LOAD_LIMIT_COUNT = "load.limit.count",
		KEY_LOAD_LIMIT_TIME = "load.limit.time",
		KEY_LOAD_LIMIT_RATE = "load.limit.rate",
		KEY_LOAD_LIMIT_REQSLEEP_MILLISEC = "load.limit.reqSleepMilliSec",
		KEY_RUN_VERSION = "run.version",
		//
		KEY_REMOTE_SERVE_JMX = "remote.serveJMX",
		KEY_REMOTE_PORT_CONTROL = "remote.port.control",
		KEY_REMOTE_PORT_MONITOR = "remote.port.monitor",
		KEY_REMOTE_PORT_WEBUI = "remote.port.webui",
		//
		KEY_STORAGE_ADDRS = "storage.addrs",
		KEY_STORAGE_SCHEME = "storage.scheme",
		KEY_STORAGE_NAMESPACE = "storage.namespace",
		//
		KEY_STORAGE_MOCK_CAPACITY = "storage.mock.capacity",
		KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT = "storage.mock.container.countLimit",
		KEY_STORAGE_MOCK_CONTAINER_CAPACITY = "storage.mock.container.capacity",
		KEY_STORAGE_MOCK_HEAD_COUNT = "storage.mock.headCount",
		KEY_STORAGE_MOCK_WORKERS_PER_SOCKET = "storage.mock.workersPerSocket",
		//
		KEY_API_NAME = "api.name",
		KEY_API_S3_BUCKET = "api.type.s3.bucket",
		KEY_API_ATMOS_SUBTENANT = "api.type.atmos.subtenant",
		KEY_API_SWIFT_AUTH_TOKEN = "api.type.swift.authToken",
		KEY_API_SWIFT_CONTAINER = "api.type.swift.container",
		//  Single
		KEY_SCENARIO_SINGLE_LOAD = "scenario.type.single.load",
		//  Chain
		KEY_SCENARIO_CHAIN_LOAD = "scenario.type.chain.load",
		KEY_SCENARIO_CHAIN_CONCURRENT = "scenario.type.chain.concurrent",
		KEY_SCENARIO_CHAIN_ITEMSBUFFER = "scenario.type.chain.itemsBuffer",
		//  Rampup
		KEY_SCENARIO_RAMPUP_SIZES = "scenario.type.rampup.sizes",
		KEY_SCENARIO_RAMPUP_CONN_COUNTS = "scenario.type.rampup.connCounts",
		//
		KEY_RUN_RESUME_ENABLED = "run.resume.enabled",
		//
		FNAME_CONF = "mongoose.json";
	//
	private static InheritableThreadLocal<RunTimeConfig>
		CONTEXT_CONFIG = new InheritableThreadLocal<>();
	private static final List<String>
		IMMUTABLE_PARAMS = new ArrayList<>();
	private static RunTimeConfig DEFAULT_INSTANCE;
	//
	public static void initContext() {
		final Logger log = LogManager.getLogger();
		final RunTimeConfig instance = new RunTimeConfig();
		DEFAULT_INSTANCE = instance;
		instance.loadProperties();
		final String
			runId = System.getProperty(KEY_RUN_ID),
			runMode = System.getProperty(KEY_RUN_MODE);
		if(runId != null && runId.length() > 0) {
			instance.set(KEY_RUN_ID, runId);
		}
		if(runMode != null && runMode.length() > 0) {
			instance.set(KEY_RUN_MODE, runMode);
		}
		setContext(instance);
		log.info(Markers.CFG, RunTimeConfig.getContext().toFormattedString());
	}
	//
	public static RunTimeConfig getDefault() {
		return (RunTimeConfig) DEFAULT_INSTANCE.clone();
	}
	//
	public void loadProperties() {
		loadJsonProps(
			Paths.get(DIR_ROOT, Constants.DIR_CONF).resolve(FNAME_CONF)
		);
		loadSysProps();
	}
	//
	public String toFormattedString() {
		final Logger log = LogManager.getLogger();
		//
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
		//
		try {
			final Object json = mapper.readValue(getJsonProps(), Object.class);
			return CFG_HEADER + mapper.writeValueAsString(json);
		} catch (final IOException e) {
			LogUtil.exception(log, Level.WARN, e, "Failed to read properties from \"{}\" file",
				Paths.get(DIR_ROOT, Constants.DIR_CONF, FNAME_CONF).toString());
		}
		return null;
	}
	//
	public static RunTimeConfig getContext() {
		final RunTimeConfig instance = CONTEXT_CONFIG.get();
		if(instance == null) {
			initContext();
		}
		return CONTEXT_CONFIG.get();
	}
	//
	public static void setContext(final RunTimeConfig instance) {
		CONTEXT_CONFIG.set(instance);
		ThreadContext.put(KEY_RUN_ID, instance.getRunId());
		ThreadContext.put(KEY_RUN_MODE, instance.getRunMode());
	}
	//
	public final static String DIR_ROOT;
	static {
		String dirRoot = System.getProperty("user.dir");
		try {
			dirRoot = new File(
				Constants.class.getProtectionDomain().getCodeSource().getLocation().toURI()
			).getParent();
		} catch(final URISyntaxException e) {
			synchronized(System.err) {
				System.err.println("Failed to determine the executable path:");
				e.printStackTrace(System.err);
			}
		}
		DIR_ROOT = dirRoot;
	}
	//
	static {
		final ClassLoader cl = RunTimeConfig.class.getClassLoader();
		final URL urlPolicy = cl.getResource("allpermissions.policy");
		if(urlPolicy == null) {
			System.err.println(
				"Failed to load security policty from mongoose-common.jar\\allpermissions.policy"
			);
		} else {
			System.setProperty("java.security.policy", urlPolicy.toString());
			System.setSecurityManager(new SecurityManager());
		}
		initImmutableParams();
	}
	//
	private static void initImmutableParams() {
		IMMUTABLE_PARAMS.add(KEY_RUN_MODE);
		IMMUTABLE_PARAMS.add(KEY_RUN_VERSION);
		IMMUTABLE_PARAMS.add(KEY_SCENARIO_NAME);
	}
	//
	public final static Map<String, String[]> MAP_OVERRIDE = new HashMap<>();
	//
	private JsonNode rootNode;
	//
	public long getSizeBytes(final String key) {
		return SizeUtil.toSize(getString(key));
	}
	//
	public String getJsonProps() {
		rootNode = new JsonConfigLoader(this).updateJsonNode();
		return rootNode.toString();
	}
	//
	public boolean isImmutableParamsChanged(final RunTimeConfig loadStateCfg) {
		for (final String param : IMMUTABLE_PARAMS) {
			if (!getString(param).equals(loadStateCfg.getString(param))) {
				return true;
			}
		}
		return false;
	}
	//
	public final synchronized void setJsonNode(final JsonNode rootNode) {
		this.rootNode = rootNode;
	}
	//
	public final synchronized JsonNode getJsonNode() {
		return rootNode;
	}
	//
	public final synchronized void set(final String key, final Object value) {
		setProperty(key, value);
		//System.setProperty(key, value);
	}
	//
	private Set<String> mongooseKeys = new HashSet<>();
	//
	public final synchronized void setMongooseKeys(final Set<String> mongooseKeys) {
		this.mongooseKeys = mongooseKeys;
	}
	//
	public final Set<String> getMongooseKeys() {
		return mongooseKeys;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static String getConnCountPerNodeParamName(final String loadType) {
		return "load.type." + loadType + ".connections";
	}
	//
	public static String getLoadWorkersParamName(final String loadType) {
		return "load.type." + loadType + ".workers";
	}
	//
	public static String getApiPortParamName(final String api) {
		return "api.type." + api + ".port";
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final String getApiName() {
		return getString(KEY_API_NAME);
	}
	//
	public final int getApiTypePort(final String api) {
		return getInt("api.type." + api + ".port");
	}
	//
	public final String getAuthId() {
		return getString("auth.id");
	}
	//
	public final String getAuthSecret() {
		return getString("auth.secret");
	}
	//
	public final long getIOBufferSizeMin() {
		return SizeUtil.toSize(getString(KEY_IO_BUFFER_SIZE_MIN));
	}
	//
	public final long getIOBufferSizeMax() {
		return SizeUtil.toSize(getString(KEY_IO_BUFFER_SIZE_MAX));
	}
	//
	public final int getBatchSize() {
		return getInt(KEY_DATA_SRC_BATCH_SIZE);
	}
	//
	public final boolean getFlagServeJMX() {
		return getBoolean(KEY_REMOTE_SERVE_JMX);
	}
	//
	public final int getRemotePortControl() {
		return getInt(KEY_REMOTE_PORT_CONTROL);
	}
	//
	public final int getRemotePortMonitor() {
		return getInt(KEY_REMOTE_PORT_MONITOR);
	}
	//
	public final int getRemotePortWebUI() {
		return getInt(KEY_REMOTE_PORT_WEBUI);
	}
	//
	public final int getLoadMetricsPeriodSec() {
		return getInt("load.metricsPeriodSec");
	}
	//
	public final String getHttpContentType() {
		return getString("http.content.type");
	}
	//
	public final boolean getHttpContentRepeatable() {
		return getBoolean("http.content.repeatable");
	}
	//
	public final boolean getHttpContentChunked() {
		return getBoolean("http.content.chunked");
	}
	//
	public final boolean getHttpPipeliningFlag() {
		return getBoolean(KEY_HTTP_PIPELINING);
	}
	//
	public final String getHttpSignMethod() {
		return getString("http.signMethod");
	}
	//
	public final boolean getReadVerifyContent() {
		return getBoolean("load.type.read.verifyContent");
	}
	//
	public final int getUpdateCountPerTime() { return getInt(KEY_LOAD_UPDATE_PER_ITEM); }
	//
	public final String getStorageProto() {
		return getString("storage.scheme");
	}
	//
	public final String getStorageNameSpace() {
		return getString(KEY_STORAGE_NAMESPACE);
	}
	//
	public final boolean getDataFileAccessEnabled() {
		return getBoolean(KEY_DATA_FS_ACCESS);
	}
	//
	public final String getDataPrefix() {
		return getString(KEY_DATA_PREFIX);
	}
	//
	public final boolean getDataVersioningEnabled() {
		return getBoolean(KEY_DATA_VERSIONING);
	}
	//
	public final boolean isDataSrcCircularEnabled() {
		return getBoolean(KEY_DATA_SRC_CIRCULAR);
	}
	//
	public final String getRunName() {
		return getString("run.name");
	}
	//
	public final String getRunVersion() {
		return getString(KEY_RUN_VERSION);
	}
	//
	public final int getLoadConnPerNode() {
		return getInt(KEY_LOAD_CONNS);
	}
	//
	public final long getLoadLimitCount() {
		return getLong(KEY_DATA_ITEM_COUNT);
	}
	//
	public final float getLoadLimitRate() {
		return getFloat(KEY_LOAD_LIMIT_RATE);
	}
	//
	public final int getLoadLimitReqSleepMilliSec() {
		return getInt(KEY_LOAD_LIMIT_REQSLEEP_MILLISEC);
	}
	//
	public final long getDataSizeMin() {
		return SizeUtil.toSize(getString(KEY_DATA_SIZE_MIN));
	}
	//
	public final long getDataSizeMax() {
		return SizeUtil.toSize(getString(KEY_DATA_SIZE_MAX));
	}
	//
	public final float getDataSizeBias() {
		return getFloat(KEY_DATA_SIZE_BIAS);
	}
	//
	public final String[] getStorageAddrs() {
		return getStringArray(KEY_STORAGE_ADDRS);
	}
	//
	public final String[] getStorageAddrsWithPorts() {
		final List<String> nodes = new ArrayList<>();
		for(String nodeAddr : getStorageAddrs()) {
			if (!nodeAddr.contains(STORAGE_PORT_SEP)) {
				nodeAddr = nodeAddr + STORAGE_PORT_SEP + getString(
					getApiPortParamName(getApiName().toLowerCase())
				);
			}
			nodes.add(nodeAddr);
		}
		return nodes.toArray(new String[nodes.size()]);
	}
	//
	public final int getSocketTimeOut() {
		return getInt("remote.socket.timeoutMilliSec");
	}
	//
	public final boolean getSocketReuseAddrFlag() {
		return getBoolean("remote.socket.reuseAddr");
	}
	//
	public final boolean getSocketKeepAliveFlag() {
		return getBoolean("remote.socket.keepalive");
	}
	//
	public final boolean getSocketTCPNoDelayFlag() {
		return getBoolean("remote.socket.tcpNoDelay");
	}
	//
	public final int getSocketLinger() {
		return getInt("remote.socket.linger");
	}
	//
	public final long getSocketBindBackLogSize() {
		return getLong("remote.socket.bindBacklogSize");
	}
	//
	public final boolean getSocketInterestOpQueued() {
		return getBoolean("remote.socket.interestOpQueued");
	}
	//
	public final long getSocketSelectInterval() {
		return getLong("remote.socket.selectInterval");
	}
	//
	public final String[] getLoadServerAddrs() {
		return getStringArray(KEY_LOAD_SERVER_ADDRS);
	}
	//
	public final boolean getFlagAssignLoadServerToNode() {
		return getBoolean(KEY_LOAD_SERVER_ASSIGN2_NODE);
	}
	//
	public final String getDataSrcFPath() {
		return getString("data.src.fpath");
	}
	//
	public final String getScenarioLang() {
		return getString("scenario.lang");
	}
	//
	public final String getScenarioName() {
		return getString(KEY_SCENARIO_NAME);
	}
	//
	public final String getScenarioDir() {
		return getString("scenario.dir");
	}
	//
	public final String getRunId() {
		return getString(KEY_RUN_ID);
	}
	//
	public final TimeUnit getLoadLimitTimeUnit() {
		return TimeUtil.getTimeUnit(getString(KEY_LOAD_LIMIT_TIME));
	}
	//
	public final long getLoadLimitTimeValue() {
		return TimeUtil.getTimeValue(getString(KEY_LOAD_LIMIT_TIME));
	}
	//
	public final String getRunMode() {
		return getString(KEY_RUN_MODE);
	}
	//
	public final int getStorageMockContainerCapacity() {
		return getInt(KEY_STORAGE_MOCK_CONTAINER_CAPACITY);
	}
	//
	public final int getStorageMockContainerCountLimit() {
		return getInt(KEY_STORAGE_MOCK_CONTAINER_COUNT_LIMIT);
	}
	//
	public final int getStorageMockCapacity() {
		return getInt(KEY_STORAGE_MOCK_CAPACITY);
	}
	//
	public final int getStorageMockHeadCount() {
		return getInt(KEY_STORAGE_MOCK_HEAD_COUNT);
	}
	//@Deprecated
	//public final int getDataRadixSize() {
	//	return getInt("data.radix.size");
	//}
	//
	//@Deprecated
	//public final int getDataRadixOffset() {
	//	return getInt("data.radix.offset");
	//}
	//
	public final int getStorageMockWorkersPerSocket() {
		return getInt(KEY_STORAGE_MOCK_WORKERS_PER_SOCKET);
	}
	//
	public final int getStorageMockMinConnLifeMilliSec() {
		return getInt("storage.mock.fault.minConnLifeMilliSec");
	}
	//
	public final int getStorageMockMaxConnLifeMilliSec() {
		return getInt("storage.mock.fault.maxConnLifeMilliSec");
	}
	//
	public final String getDataSrcRingSeed() {
		return getString(KEY_DATA_SRC_RING_SEED);
	}
	//
	public final long getDataSrcRingSize() {
		return SizeUtil.toSize(getString(KEY_DATA_SRC_RING_SIZE));
	}
	//
	public final long getDataSrcRingUniformSize() {
		long uniformSize = 0, ringSize = getDataSrcRingSize();
		try {
			uniformSize = SizeUtil.toSize(getString(KEY_DATA_SRC_RING_UNIFORM_SIZE));
		} catch(final Exception e) {
			LogUtil.exception(
				LogManager.getLogger(), Level.DEBUG, e,
				"Failed to get the value for the parameter \"{}\"", KEY_DATA_SRC_RING_UNIFORM_SIZE
			);
		}
		if(uniformSize < 1 || uniformSize > ringSize) {
			uniformSize = ringSize;
		}
		return uniformSize;
	}
	//
	public final int getWorkerCountFor(final String loadType) {
		return getInt(getLoadWorkersParamName(loadType));
	}
	//
	public final int getConnCountPerNodeFor(final String loadType) {
		return getInt(getConnCountPerNodeParamName(loadType));
	}
	//
	public final String getApiS3AuthPrefix() {
		return getString("api.type.s3.authPrefix");
	}
	//
	public final String getScenarioSingleLoad() {
		return getString(KEY_SCENARIO_SINGLE_LOAD);
	}
	//
	public final String[] getScenarioChainLoad() {
		return getStringArray(KEY_SCENARIO_CHAIN_LOAD);
	}
	//
	public final boolean getScenarioChainConcurrentFlag() {
		return getBoolean(KEY_SCENARIO_CHAIN_CONCURRENT);
	}
	//
	public final String[] getScenarioRampupConnCounts() {
		return getStringArray(KEY_SCENARIO_RAMPUP_CONN_COUNTS);
	}
	//
	public final String[] getScenarioRampupSizes() {
		return getStringArray(KEY_SCENARIO_RAMPUP_SIZES);
	}
	//
	public final String getWebUIWSTimeout() {
		return getString("remote.webui.wsTimeOut.value") + "." + getString("remote.webui.wsTimeOut.unit");
	}
	//
	public final String getWebUIWSTimeOutValue() {
		return getString("remote.webui.wsTimeOut.value");
	}
	//
	public final String getWebUIWSTimeOutUnit() {
		return getString("remote.webui.wsTimeOut.unit");
	}
	//
	public final boolean isShuffleItemsEnabled() {return  getBoolean(KEY_DATA_SRC_RANDOM);}
	//
	public final boolean isRunResumeEnabled() {
		return getBoolean(KEY_RUN_RESUME_ENABLED);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final synchronized void writeExternal(final ObjectOutput out)
	throws IOException {
		final Logger log = LogManager.getLogger();
		log.debug(Markers.MSG, "Going to upload properties to a server");
		String nextPropName;
		Object nextPropValue;
		final HashMap<String, String> propsMap = new HashMap<>();
		for(final Iterator<String> i = getKeys(); i.hasNext();) {
			nextPropName = i.next();
			nextPropValue = getProperty(nextPropName);
			log.trace(
				Markers.MSG, "Write property: \"{}\" = \"{}\"", nextPropName, nextPropValue
			);
			if(List.class.isInstance(nextPropValue)) {
				propsMap.put(
					nextPropName,
					StringUtils.join(List.class.cast(nextPropValue), LIST_SEP)
				);
			} else if(String.class.isInstance(nextPropValue)) {
				propsMap.put(nextPropName, String.class.cast(nextPropValue));
			} else if(Number.class.isInstance(nextPropValue)) {
				propsMap.put(nextPropName, Number.class.cast(nextPropValue).toString());
			} else if(nextPropValue != null) {
				propsMap.put(nextPropName, nextPropValue.toString());
			} else {
				log.debug(Markers.ERR, "Property \"{}\" value is null", nextPropName);
			}
		}
		//
		log.trace(Markers.MSG, "Sending configuration: {}", propsMap);
		//
		out.writeObject(propsMap);
		log.debug(Markers.MSG, "Uploaded the properties from client side");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final synchronized void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final Logger log = LogManager.getLogger();
		log.debug(Markers.MSG, "Going to fetch the properties from client side");
		final HashMap<String, String> confMap = HashMap.class.cast(in.readObject());
		log.trace(Markers.MSG, "Got the properties from client side: {}", confMap);
		//
		final String
			serverVersion = CONTEXT_CONFIG.get().getRunVersion(),
			clientVersion = confMap.get(KEY_RUN_VERSION);
		if(serverVersion.equals(clientVersion)) {
			// put the properties into the System
			Object nextPropValue;
			final RunTimeConfig localRunTimeConfig = CONTEXT_CONFIG.get();
			for(final String nextPropName : confMap.keySet()) {
				// do not override the JMX port from the load client side
				// in order to allow to run both client and server on the same host
				if(nextPropName.startsWith(KEY_REMOTE_PORT_MONITOR)) {
					nextPropValue = localRunTimeConfig.getProperty(nextPropName);
				} else {
					nextPropValue = confMap.get(nextPropName);
				}
				log.trace(Markers.MSG, "Read property: \"{}\" = \"{}\"", nextPropName, nextPropValue);
				if(List.class.isInstance(nextPropValue)) {
					setProperty(
						nextPropName,
						StringUtils.join(List.class.cast(nextPropValue), LIST_SEP)
					);
				} else if(nextPropValue != null) {
					setProperty(nextPropName, nextPropValue);
					//setProperty(nextPropName, String.class.cast(nextPropValue));
				} else {
					log.debug(Markers.ERR, "Property \"{}\" value is null", nextPropName);
				}
			}
			CONTEXT_CONFIG.set(this);
		} else {
			final String errMsg = String.format(
				"%s, version mismatch, server: %s client: %s",
				getRunName(), serverVersion, clientVersion
			);
			log.fatal(Markers.ERR, errMsg);
			throw new IOException(errMsg);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized void loadJsonProps(final Path filePath) {
		final Logger log = LogManager.getLogger();
		new JsonConfigLoader(this).loadPropsFromJsonCfgFile(filePath);
		log.debug(Markers.MSG, "Going to override the aliasing section");
		for(final String key : mongooseKeys) {
			if(key.startsWith(PREFIX_KEY_ALIASING)) {
				final String correctKey = key.replaceAll(PREFIX_KEY_ALIASING, "");
				log.trace(
					Markers.MSG, "Alias: \"{}\" -> \"{}\"", correctKey, getStringArray(key)
				);
				MAP_OVERRIDE.put(correctKey, getStringArray(key));
			}
		}
	}
	//
	public synchronized void loadSysProps() {
		final Logger log = LogManager.getLogger();
		final SystemConfiguration sysProps = new SystemConfiguration();
		String key, keys2override[];
		Object sharedValue;
		for(final Iterator<String> keyIter = sysProps.getKeys(); keyIter.hasNext();) {
			key = keyIter.next();
			log.trace(
				Markers.MSG, "System property: \"{}\": \"{}\" -> \"{}\"",
				key, getProperty(key), sysProps.getProperty(key)
			);
			keys2override = MAP_OVERRIDE.get(key);
			sharedValue = sysProps.getProperty(key);
			setProperty(key, sharedValue);
			if(keys2override != null) {
				for(final String key2override: keys2override) {
					setProperty(key2override, sharedValue);
				}
			}
		}
	}
	//
	public void overrideSystemProperties(Map<String, String> props){
		for(Map.Entry<String, String> entry : props.entrySet()){
			setProperty(entry.getKey(), entry.getValue());
		}
	}
	//
	public final static String
		CFG_HEADER = "Current configuration:\n";
	private final static String
		TABLE_BORDER = "\n+--------------------------------+----------------------------------------------------------------+",
		TABLE_HEADER = "Configuration parameters:";
	//
	@Override
	public final String toString() {
		String nextKey;
		Object nextVal;
		final StrBuilder strBuilder = new StrBuilder()
			.append(TABLE_HEADER).append(TABLE_BORDER)
			.appendNewLine().append("| ").appendFixedWidthPadRight("Key", 31, ' ')
			.append("| ").appendFixedWidthPadRight("Value", 63, ' ').append('|')
			.append(TABLE_BORDER);
		for(
			final Iterator<String> keyIterator = getKeys();
			keyIterator.hasNext();
		) {
			nextKey = keyIterator.next();
			nextVal = getProperty(nextKey);
			switch(nextKey) {
				case KEY_RUN_ID:
				case KEY_RUN_MODE:
				case KEY_SCENARIO_NAME:
				case KEY_LOAD_LIMIT_TIME:
				case KEY_RUN_VERSION:
				case KEY_DATA_ITEM_COUNT:
				case KEY_DATA_SIZE:
				case KEY_DATA_SRC_RING_SEED:
				case KEY_DATA_SRC_RING_SIZE:
				case KEY_LOAD_CONNS:
				case KEY_LOAD_WORKERS:
				case KEY_STORAGE_ADDRS:
				case KEY_API_NAME:
					strBuilder
						.appendNewLine().append("| ")
						.appendFixedWidthPadRight(nextKey, 31, ' ')
						.append("| ")
						.appendFixedWidthPadRight(nextVal, 63, ' ')
						.append('|');
					break;
			}
		}
		return strBuilder.append(TABLE_BORDER).toString();
	}
}
