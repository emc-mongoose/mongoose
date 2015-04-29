package com.emc.mongoose.common.conf;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
//
import com.fasterxml.jackson.databind.JsonNode;
//
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
//
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
import java.util.Calendar;
import java.util.HashMap;
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
		//
		KEY_AUTH_ID = "auth.id",
		KEY_AUTH_SECRET = "auth.secret",
		//
		KEY_DATA_ITEM_COUNT = "load.limit.count",
		KEY_DATA_COUNT = "data.count",
		KEY_DATA_SIZE = "data.size",
		KEY_DATA_SIZE_MIN = "data.size.min",
		KEY_DATA_SIZE_MAX = "data.size.max",
		KEY_DATA_SIZE_BIAS = "data.size.bias",
		KEY_DATA_RING_SEED = "data.buffer.ring.seed",
		KEY_DATA_RING_SIZE = "data.buffer.ring.size",
		KEY_DATA_SRC_FPATH = "data.src.fpath",
		//
		KEY_LOAD_THREADS = "load.threads",
		KEY_LOAD_UPDATE_PER_ITEM = "load.type.update.perItem",
		//
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode",
		KEY_RUN_TIME = "run.time",
		KEY_RUN_TIMESTAMP = "run.timestamp",
		KEY_SCENARIO_NAME = "scenario.name",
		KEY_LOAD_METRICS_PERIOD_SEC = "load.metricsPeriodSec",
		KEY_LOAD_LIMIT_COUNT = "load.limit.count",
		KEY_LOAD_LIMIT_TIME = "load.limit.time",
		KEY_LOAD_TIME = "load.time",
		KEY_RUN_VERSION = "run.version",
		//
		KEY_STORAGE_ADDRS = "storage.addrs",
		KEY_STORAGE_FS_ACCESS = "storage.fsAccess",
		KEY_STORAGE_SCHEME = "storage.scheme",
		KEY_STORAGE_NAMESPACE = "storage.namespace",
		//
		KEY_API_NAME = "api.name",
		KEY_API_S3_BUCKET = "api.type.s3.bucket",
		//
		//  Single
		KEY_SCENARIO_SINGLE_LOAD = "scenario.type.single.load",
		//  Chain
		KEY_SCENARIO_CHAIN_LOAD = "scenario.type.chain.load",
		KEY_SCENARIO_CHAIN_CONCURRENT = "scenario.type.chain.concurrent",
		KEY_SCENARIO_CHAIN_ITEMSBUFFER = "scenario.type.chain.itemsBuffer",
		//  Rampup
		KEY_SCENARIO_RAMPUP_SIZES = "scenario.type.rampup.sizes",
		KEY_SCENARIO_RAMPUP_THREAD_COUNTS = "scenario.type.rampup.threadCounts",
		//  For ui property tree
		KEY_CHILDREN_PROPS = "children",
		//
		FNAME_CONF = "mongoose.json";
	//
	private static InheritableThreadLocal<RunTimeConfig>
		CONTEXT_CONFIG = new InheritableThreadLocal<>();
	//
	public static void initContext() {
		RunTimeConfig instance = RunTimeConfig.getContext();
		if(instance == null) {
			instance = new RunTimeConfig();
			instance.set(KEY_RUN_ID, System.getProperty(KEY_RUN_ID));
			instance.set(KEY_RUN_MODE, System.getProperty(KEY_RUN_MODE));
			setContext(instance);
		}
	}
	//
	public static RunTimeConfig getContext() {
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
	}
	//
	private final static Map<String, String[]> MAP_OVERRIDE = new HashMap<>();
	//
	private final Map<String, Object> properties = new HashMap<>();
	//
	private JsonNode rootNode;
	//
	public long getSizeBytes(final String key) {
		return SizeUtil.toSize(getString(key));
	}
	//
	public String getJsonProps() {
		JsonConfigLoader.updateProps(
			Paths.get(DIR_ROOT, Constants.DIR_CONF)
				.resolve(FNAME_CONF),
			this, false
		);
		return rootNode.toString();
	}
	//
	public final synchronized void putJsonProps(final JsonNode rootNode) {
		this.rootNode = rootNode;
	}
	//
	public final synchronized void set(final String key, final Object value) {
		setProperty(key, value);
		//System.setProperty(key, value);
	}
	//
	private Set<String> mongooseKeys;
	//
	public final synchronized void setMongooseKeys(final Set<String> mongooseKeys) {
		this.mongooseKeys = mongooseKeys;
	}
	//
	public final Set<String> getMongooseKeys() {
		return mongooseKeys;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static String getLoadThreadsParamName(final String loadType) {
		return "load.type." + loadType + ".threads";
	}
	//
	public static String getApiPortParamName(final String api) {
		return "api.type." + api + ".port";
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final int getRunReqTimeOutMilliSec() {
		return getInt("run.request.timeoutMilliSec");
	}
	//
	public final int getRunRetryDelayMilliSec() {
		return getInt("run.retry.delayMilliSec");
	}
	//
	public final int getRunRetryCountMax() {
		return getInt("run.retry.countMax");
	}

	//
	public final boolean getRunRequestRetries() {
		return getBoolean("run.request.retries");
	}

	//
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
	public final long getDataBufferSize() {
		return SizeUtil.toSize(getString("data.buffer.size"));
	}
	//
	public final long getDataRingSize() {
		return SizeUtil.toSize(getString("data.buffer.ring.size"));
	}

	//
	public final int getRemotePortControl() {
		return getInt("remote.port.control");
	}
	//
	public final int getRemotePortExport() {
		return getInt("remote.port.export");
	}
	//
	public final int getRemotePortImport() {
		return getInt("remote.port.import");
	}
	//
	public final int getRemotePortWebUI() {
		return getInt("remote.port.webui");
	}
	//
	public final int getLoadMetricsPeriodSec() {
		return getInt("load.metricsPeriodSec");
	}
	//
	public final int getRunRequestQueueSize() {
		return getInt("run.request.queueSize");
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
	public final boolean getReadVerifyContent() {
		return getBoolean("load.type.read.verifyContent");
	}

	//
	public final String getStorageProto() {
		return getString("storage.scheme");
	}
	//
	public final String getStorageNameSpace() {
		return getString("storage.namespace");
	}
	//
	public final String getHttpSignMethod() {
		return getString("http.signMethod");
	}
	//
	public final boolean getStorageFileAccessEnabled() {
		return getBoolean(KEY_STORAGE_FS_ACCESS);
	}
	//
	public final boolean getStorageVersioningEnabled() {
		return getBoolean("storage.versioning");
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
	public final long getLoadLimitCount() {
		return getLong(KEY_DATA_ITEM_COUNT);
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
	public final int getConnPoolTimeOut() {
		return getInt("remote.connection.poolTimeoutMilliSec");
	}
	//
	public final int getConnTimeOut() {
		return getInt("remote.connection.timeoutMilliSec");
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
		return getBoolean("remote.socket.tcpNodelay");
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
	public final String[] getLoadServers() {
		return getStringArray("load.servers");
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
	public final int getStorageMockCapacity() {
		return getInt("storage.mock.capacity");
	}
	//
	public final int getStorageMockHeadCount() {
		return getInt("storage.mock.headCount");
	}
	//
	public final int getDataRadixSize() {
		return getInt("data.radix.size");
	}
	//
	public final int getDataRadixOffset() {
		return getInt("data.radix.offset");
	}
	//
	public final int getStorageMockIoThreadsPerSocket() {
		return getInt("storage.mock.ioThreadsPerSocket");
	}
	//
	public final int getStorageMockFaultSleepMilliSec() {
		return getInt("storage.mock.fault.sleepMilliSec");
	}
	//
	public final int getStorageMockFaultPeriod() {
		return getInt("storage.mock.fault.period");
	}

	//
	public final String getDataBufferRingSeed() {
		return getString("data.buffer.ring.seed");
	}
	//
	public final long getDataBufferRingSize() {
		return SizeUtil.toSize(getString("data.buffer.ring.size"));
	}
	//
	public final short getLoadTypeThreads(final String loadType) {
		return getShort("load.type." + loadType + ".threads");
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
	public final String[] getScenarioRampupThreadCounts() {
		return getStringArray(KEY_SCENARIO_RAMPUP_THREAD_COUNTS);
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
	// for persister
	public final boolean getPersistEnabled(){
		return getBoolean("persist.enabled", false);
	}
	//
	public final String getPersistDatabaseProvider(){
		return getString("persist.database.provider");
	}
	//
	public final String getPersistDatabaseDriver(){
		return getString("persist.database.driver");
	}
	//
	public final String getPersistDatabaseUsername(){
		return getString("persist.database.username");
	}
	//
	public final String getPersistDatabasePassword(){
		return getString("persist.database.password");
	}
	//
	public final String getPersistDatabaseAddr(){
		return getString("persist.database.addr");
	}
	//
	public final String getPersistDatabasePort(){
		return getString("persist.database.port");
	}
	//
	public final String getPersistDatabaseName(){
		return getString("persist.database.name");
	}
	//
	public final int getPersistExecutorPoolSize(){
		return getInt("persist.executor.pool.size", 1000);
	}
	//
	public final int getPersistExecutorPoolTimeout(){
		return getInt("persist.executor.pool.timeout", 0);
	}
	//
	public final int getPersistExecutorQueueSize(){
		return getInt("persist.executor.queueSize", 1000000);
	}
	//
	public final int getPersistMaxActiveConnection(){
		return getInt("persist.maxActiveConnection", 100);
	}
	//
	public final int getPersistConnectionTimeout(){
		return getInt("persist.connectionTimeout", 0);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final synchronized void writeExternal(final ObjectOutput out)
	throws IOException {
		final Logger log = LogManager.getLogger();
		log.debug(LogUtil.MSG, "Going to upload properties to a server");
		String nextPropName;
		Object nextPropValue;
		final HashMap<String, String> propsMap = new HashMap<>();
		for(final Iterator<String> i = getKeys(); i.hasNext();) {
			nextPropName = i.next();
			nextPropValue = getProperty(nextPropName);
			log.trace(
				LogUtil.MSG, "Write property: \"{}\" = \"{}\"", nextPropName, nextPropValue
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
			} else if(nextPropValue == null) {
				log.warn(LogUtil.ERR, "Property \"{}\" is null");
			} else {
				log.error(
					LogUtil.ERR, "Unexpected type \"{}\" for property \"{}\"",
					nextPropValue.getClass().getCanonicalName(), nextPropName
				);
			}
		}
		//
		log.trace(LogUtil.MSG, "Sending configuration: {}", propsMap);
		//
		out.writeObject(propsMap);
		log.debug(LogUtil.MSG, "Uploaded the properties from client side");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final synchronized void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final Logger log = LogManager.getLogger();
		log.debug(LogUtil.MSG, "Going to fetch the properties from client side");
		final HashMap<String, String> confMap = HashMap.class.cast(in.readObject());
		log.trace(LogUtil.MSG, "Got the properties from client side: {}", confMap);
		//
		final String
			serverVersion = CONTEXT_CONFIG.get().getRunVersion(),
			clientVersion = confMap.get(KEY_RUN_VERSION);
		if(serverVersion.equals(clientVersion)) {
			// put the properties into the System
			Object nextPropValue;
			final RunTimeConfig localRunTimeConfig = CONTEXT_CONFIG.get();
			for(final String nextPropName: confMap.keySet()) {
				// to not to override the import/export ports from the load client side
				nextPropValue = nextPropName.startsWith("remote") ?
					localRunTimeConfig.getString(nextPropName) :
					confMap.get(nextPropName);
				log.trace(LogUtil.MSG, "Read property: \"{}\" = \"{}\"", nextPropName, nextPropValue);
				if(List.class.isInstance(nextPropValue)) {
					setProperty(
						nextPropName,
						StringUtils.join(List.class.cast(nextPropValue), LIST_SEP)
					);
				} else if(String.class.isInstance(nextPropValue)) {
					setProperty(nextPropName, String.class.cast(nextPropValue));
				} else if(nextPropValue == null) {
					log.debug(LogUtil.ERR, "Property \"{}\" is null", nextPropName);
				} else {
					log.error(
						LogUtil.ERR, "Unexpected type \"{}\" for property \"{}\"",
						nextPropValue.getClass().getCanonicalName(), nextPropName
					);
				}
			}
			CONTEXT_CONFIG.set(this);
			log.info(LogUtil.MSG, toString());
		} else {
			final String errMsg = String.format(
				"%s, version mismatch, server: %s client: %s",
				getRunName(), serverVersion, clientVersion
			);
			log.fatal(LogUtil.ERR, errMsg);
			throw new IOException(errMsg);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized void loadPropsFromJsonCfgFile(final Path propsDir) {
		JsonConfigLoader.loadPropsFromJsonCfgFile(propsDir, this);
        for (String key : mongooseKeys) {
            if (key.startsWith("aliasing.")) {
                final String correctKey = key.replaceAll("aliasing.", "");
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
				LogUtil.MSG, "System property: \"{}\": \"{}\" -> \"{}\"",
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
	@Override
	public synchronized RunTimeConfig clone() {
		final RunTimeConfig runTimeConfig = RunTimeConfig.class.cast(super.clone());
		if(runTimeConfig != null) {
			runTimeConfig.set(
				KEY_RUN_ID,
				LogUtil.FMT_DT.format(Calendar.getInstance(LogUtil.TZ_UTC, LogUtil.LOCALE_DEFAULT).getTime())
			);
		}
		return runTimeConfig;
	}
	//
	public void overrideSystemProperties(Map<String, String> props){
		for(Map.Entry<String, String> entry : props.entrySet()){
			setProperty(entry.getKey(), entry.getValue());
		}
	}
	//
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
				case KEY_DATA_RING_SEED:
				case KEY_DATA_RING_SIZE:
				case KEY_LOAD_THREADS:
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
