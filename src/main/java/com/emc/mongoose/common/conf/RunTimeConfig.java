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
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
		KEY_DATA_ITEM_COUNT = "load.limit.dataItemCount",
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
		KEY_SCENARIO_NAME = "scenario.name",
		KEY_LOAD_METRICS_PERIOD_SEC = "load.metricsPeriodSec",
		KEY_LOAD_TIME = "load.time",
		KEY_LOAD_LIMIT_TIME = "load.limit.time",
		KEY_LOAD_LIMIT_TIME_VALUE = KEY_LOAD_LIMIT_TIME + ".value",
		KEY_LOAD_LIMIT_TIME_UNIT = KEY_LOAD_LIMIT_TIME + ".unit",
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
		KEY_SCENARIO_CHAIN_SIMULTANEOUS = "scenario.type.chain.simultaneous",
		KEY_SCENARIO_CHAIN_ITEMSBUFFER = "scenario.type.chain.itemsbuffer",
		//  Rampup
		KEY_SCENARIO_RAMPUP_SIZES = "scenario.type.rampup.sizes",
		KEY_SCENARIO_RAMPUP_THREAD_COUNTS = "scenario.type.rampup.threadCounts",
		//  For ui property tree
		KEY_CHILDREN_PROPS = "children",
		//
		FNAME_CONF = "properties.json";
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
	static {
		// shortcuts
		MAP_OVERRIDE.put(KEY_DATA_SIZE, new String[] {"data.size.min", "data.size.max"});
		MAP_OVERRIDE.put(KEY_LOAD_TIME, new String[] {KEY_LOAD_LIMIT_TIME_VALUE, KEY_LOAD_LIMIT_TIME_UNIT});
		MAP_OVERRIDE.put(KEY_LOAD_THREADS, new String[] {"load.type.append.threads", "load.type.create.threads", "load.type.read.threads", "load.type.update.threads", "load.type.delete.threads"});
		// backward compatibility
		MAP_OVERRIDE.put(KEY_RUN_TIME, new String[] {KEY_LOAD_TIME});
		MAP_OVERRIDE.put(KEY_DATA_COUNT, new String[] {"load.limit.dataItemCount"});
		MAP_OVERRIDE.put("load.drivers", new String[] {"load.servers"});
	}
	//
	private final static String
		SIZE_UNITS = "kmgtpe",
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	private final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)b?");
	//
	private final Map<String, Object> properties = new HashMap<>();
	//
	private JsonNode rootNode;
	//
	public long getSizeBytes(final String key) {
		return toSize(getString(key));
	}
	//
	public static long toSize(final String value) {
		final String unit;
		final Matcher matcher = PATTERN_SIZE.matcher(value.toLowerCase());
		long size, degree;
		if(matcher.matches() && matcher.groupCount() > 0 && matcher.groupCount() < 3) {
			size = Long.valueOf(matcher.group(1), 10);
			unit = matcher.group(2);
			if(unit.length() == 0) {
				degree = 0;
			} else if(unit.length() == 1) {
				degree = SIZE_UNITS.indexOf(matcher.group(2)) + 1;
			} else {
				throw new IllegalArgumentException(
					String.format(FMT_MSG_INVALID_SIZE, value, PATTERN_SIZE)
				);
			}
		} else {
			throw new IllegalArgumentException(
				String.format(FMT_MSG_INVALID_SIZE, value, PATTERN_SIZE)
			);
		}
		size *= 1L << 10 * degree;
		return size;
	}
	//
	public static String formatSize(final long v) {
		if(v < 1024) {
			return v + "B";
		}
		final int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		final double x = (double) v / (1L << (z * 10));
		return String.format(
			Locale.ROOT,
			x < 10 ? "%.3f%sb" : x < 100 ? "%.2f%sb" : "%.1f%sb",
			x, z > 0 ? SIZE_UNITS.charAt(z - 1) : ""
		).toUpperCase();
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
		return getSizeBytes("data.buffer.size");
	}
	//
	public final long getDataRingSize() {
		return getSizeBytes("data.buffer.ring.size");
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
	public final long getLoadLimitDataItemCount() {
		return getLong(KEY_DATA_ITEM_COUNT);
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
		return TimeUnit.valueOf(getString(KEY_LOAD_LIMIT_TIME + ".unit").toUpperCase());
	}
	//
	public final long getLoadLimitTimeValue() {
		return getLong(KEY_LOAD_LIMIT_TIME + ".value");
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
		return getInt("storage.mock.iothreadsPersocket");
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
		return getSizeBytes("data.buffer.ring.size");
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
			if (key.equals(RunTimeConfig.KEY_LOAD_TIME)) {
				setLoadTimeSpecificProps(sysProps.getString(key));
			} else {
				if(keys2override != null) {
					for(final String key2override: keys2override) {
						setProperty(key2override, sharedValue);
						if (key2override.equals(KEY_LOAD_TIME)) {
							setLoadTimeSpecificProps(sharedValue.toString());
						}
					}
				}
			}
		}
	}
	//
	private void setLoadTimeSpecificProps(final String sharedValue) {
		final Logger log = LogManager.getLogger();
		final String[] loadTime = sharedValue.split("\\.");
		final String[] loadTimeKeys = MAP_OVERRIDE.get(KEY_LOAD_TIME);
		if (loadTime.length > 1) {
			for (int i = 0; i < loadTimeKeys.length; i++) {
				setProperty(loadTimeKeys[i], loadTime[i]);
			}
		} else {
			log.error(LogUtil.ERR, "Load time isn't correct. Default values will be used.");
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
				case RunTimeConfig.KEY_RUN_ID:
				case RunTimeConfig.KEY_RUN_MODE:
				case RunTimeConfig.KEY_SCENARIO_NAME:
				case RunTimeConfig.KEY_LOAD_LIMIT_TIME_VALUE:
				case RunTimeConfig.KEY_LOAD_LIMIT_TIME_UNIT:
				case RunTimeConfig.KEY_RUN_VERSION:
				case RunTimeConfig.KEY_DATA_ITEM_COUNT:
				case RunTimeConfig.KEY_DATA_SIZE:
				case RunTimeConfig.KEY_DATA_RING_SEED:
				case RunTimeConfig.KEY_DATA_RING_SIZE:
				case RunTimeConfig.KEY_LOAD_THREADS:
				case RunTimeConfig.KEY_STORAGE_ADDRS:
				case RunTimeConfig.KEY_API_NAME:
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
