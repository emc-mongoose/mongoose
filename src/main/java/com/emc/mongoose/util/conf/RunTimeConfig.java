package com.emc.mongoose.util.conf;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.TraceLogger;
//
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
	private static InheritableThreadLocal<RunTimeConfig>
		INHERITABLE_CONTEXT = new InheritableThreadLocal<>();
	//
	public static RunTimeConfig getContext() {
		return INHERITABLE_CONTEXT.get();
	}
	//
	public static void setContext(final RunTimeConfig instance) {
		INHERITABLE_CONTEXT.set(instance);
	}
	//
	private Set<String> mongooseKeys;
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		LIST_SEP = ",",
		//
		KEY_DATA_COUNT = "data.count",
		KEY_DATA_SIZE = "data.size",
		KEY_DATA_SIZE_BIAS = "data.size.bias",
		KEY_DATA_RING_SEED = "data.ring.seed",
		KEY_DATA_RING_SIZE = "data.ring.size",
		//
		KEY_LOAD_THREADS = "load.threads",
		KEY_LOAD_TIME = "load.step.time",
		//
		KEY_RUN_ID = "run.id",
		KEY_RUN_MODE = "run.mode",
		KEY_RUN_SCENARIO_NAME = "run.scenario.name",
		KEY_RUN_METRICS_PERIOD_SEC = "run.metrics.period.sec",
		KEY_RUN_TIME = "run.time",
		KEY_RUN_VERSION = "run.version",
		//
		KEY_STORAGE_ADDRS = "storage.addrs",
		KEY_STORAGE_API = "storage.api";
	//
	private final static Map<String, String[]> MAP_OVERRIDE = new HashMap<>();
	//
	static {
		MAP_OVERRIDE.put(KEY_DATA_SIZE, new String[] {"data.size.min", "data.size.max"});
		MAP_OVERRIDE.put(KEY_LOAD_TIME, new String[] {KEY_RUN_TIME});
		MAP_OVERRIDE.put(KEY_LOAD_THREADS, new String[] {"load.append.threads", "load.create.threads", "load.read.threads", "load.update.threads", "load.delete.threads"});
		MAP_OVERRIDE.put("remote.drivers", new String[] {"remote.servers"});
	}
	//
	private final static String
		SIZE_UNITS = "kmgtpe",
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	private final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)b?");
	//
	private final Map<String, Object> properties = new HashMap<>();
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
	public String getPropertiesMap() {
		DirectoryLoader.updatePropertiesFromDir(Paths.get(Main.DIR_ROOT, Main.DIR_CONF, Main.DIR_PROPERTIES), this);
		final ObjectMapper mapper = new ObjectMapper();
		try {
			return mapper.writeValueAsString(properties);
		} catch (final JsonProcessingException e) {
			TraceLogger.failure(LOG, Level.ERROR, e, "Failed json processing");
		}
		return null;
	}
	//
	@SuppressWarnings("unchecked")
	public final synchronized void put(
		final List<String> dirs, final String fileName,
		final List<DefaultMapEntry<String, Object>> props
	) {
		Map<String, Object> node = properties;
		if (dirs != null) {
			for (final String nextDir : dirs) {
				if (!node.containsKey(nextDir)) {
					node.put(nextDir, new LinkedHashMap<>());
				}
				node = (Map<String, Object>) node.get(nextDir);
			}
		}
		node.put(fileName, props);
	}
	//
	public final synchronized void set(final String key, final Object value) {
		setProperty(key, value);
		//System.setProperty(key, value);
	}
	//
	public final synchronized void setMongooseKeys(final Set<String> mongooseKeys) {
		this.mongooseKeys = mongooseKeys;
	}
	//
	public final Set<String> getMongooseKeys() {
		return mongooseKeys;
	}
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final int getRunReqTimeOutMilliSec() {
		return getInt("run.request.timeout.millisec");
	}
	//
	public final int getRunRetryDelayMilliSec() {
		return getInt("run.retry.delay.millisec");
	}
	//
	public final int getRunRetryCountMax() {
		return getInt("run.retry.count.max");
	}
	//
	public final boolean getRunRequestRetries() {
		return getBoolean("run.request.retries");
	}
	//
	public final String getStorageApi() {
		return getString(KEY_STORAGE_API);
	}
	//
	public final int getApiPort(final String api) {
		return getInt("api." + api + ".port");
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
	public final long getDataPageSize() {
		return getSizeBytes("data.page.size");
	}
	//
	public final int getRemoteControlPort() {
		return getInt("remote.control.port");
	}
	//
	public final int getRemoteExportPort() {
		return getInt("remote.export.port");
	}
	//
	public final int getRemoteImportPort() {
		return getInt("remote.import.port");
	}
	//
	public final int getWUISvcPort() {
		return getInt("remote.wuisvc.port");
	}
	//
	public final int getRunMetricsPeriodSec() {
		return getInt("run.metrics.period.sec");
	}
	//
	public final int getRunRequestQueueSize() {
		return getInt("run.request.queue.size");
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
		return getBoolean("load.read.verify.content");
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
		return getString("http.sign.method");
	}
	//
	public final boolean getEmcFileSystemAccessEnabled() {
		return getBoolean("emc.fs.access");
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
	public final long getDataCount() {
		return getLong(KEY_DATA_COUNT);
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
		return getInt("storage.connection.pool.timeout.millisec");
	}
	//
	public final int getConnTimeOut() {
		return getInt("storage.connection.timeout.millisec");
	}
	//
	public final int getSocketTimeOut() {
		return getInt("storage.socket.timeout.millisec");
	}
	//
	public final boolean getSocketReuseAddrFlag() {
		return getBoolean("storage.socket.reuse.addr");
	}
	//
	public final boolean getSocketKeepAliveFlag() {
		return getBoolean("storage.socket.keepalive");
	}
	//
	public final boolean getSocketTCPNoDelayFlag() {
		return getBoolean("storage.socket.tcp.nodelay");
	}
	//
	public final int getSocketLinger() {
		return getInt("storage.socket.linger");
	}
	//
	public final long getSocketBindBackLogSize() {
		return getLong("storage.socket.bind.backlog.size");
	}
	//
	public final boolean getSocketInterestOpQueued() {
		return getBoolean("storage.socket.interest.op.queued");
	}
	//
	public final long getSocketSelectInterval() {
		return getLong("storage.socket.select.interval");
	}
	//
	public final String[] getRemoteServers() {
		return getStringArray("remote.servers");
	}
	//
	public final String getDataSrcFPath() {
		return getString("data.src.fpath");
	}
	//
	public final String getRunScenarioLang() {
		return getString("run.scenario.lang");
	}
	//
	public final String getRunScenarioName() {
		return getString(KEY_RUN_SCENARIO_NAME);
	}
	//
	public final String getRunScenarioDir() {
		return getString("run.scenario.dir");
	}
	//
	public final String getRunId() {
		return getString(KEY_RUN_ID);
	}
	//
	public final String getRunTime() {
		return getString(KEY_RUN_TIME);
	}
	//
	private String getFromRunTime(final int index) { return getRunTime().split("\\.")[index];}
	//
	public final TimeUnit getRunTimeUnit() { return TimeUnit.valueOf(getFromRunTime(1).toUpperCase());}
	//
	public final long getRunTimeValue() {return Long.valueOf(getFromRunTime(0));}
	//
	public final String getRunMode() {
		return getString(KEY_RUN_MODE);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final synchronized void writeExternal(final ObjectOutput out)
		throws IOException {
		LOG.debug(Markers.MSG, "Going to upload properties to a server");
		String nextPropName;
		Object nextPropValue;
		final HashMap<String, String> propsMap = new HashMap<>();
		for(final Iterator<String> i = getKeys(); i.hasNext();) {
			nextPropName = i.next();
			nextPropValue = getProperty(nextPropName);
			LOG.trace(
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
			} else if(nextPropValue == null) {
				LOG.warn(Markers.ERR, "Property \"{}\" is null");
			} else {
				LOG.error(
					Markers.ERR, "Unexpected type \"{}\" for property \"{}\"",
					nextPropValue.getClass().getCanonicalName(), nextPropName
				);
			}
		}
		//
		LOG.trace(Markers.MSG, "Sending configuration: {}", propsMap);
		//
		out.writeObject(propsMap);
		LOG.debug(Markers.MSG, "Uploaded the properties from client side");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final synchronized void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
		LOG.debug(Markers.MSG, "Going to fetch the properties from client side");
		final HashMap<String, String> confMap = HashMap.class.cast(in.readObject());
		LOG.trace(Markers.MSG, "Got the properties from client side: {}", confMap);
		//
		final String
			serverVersion = INHERITABLE_CONTEXT.get().getRunVersion(),
			clientVersion = confMap.get(KEY_RUN_VERSION);
		if(serverVersion.equals(clientVersion)) {
			// put the properties into the System
			Object nextPropValue;
			final RunTimeConfig localRunTimeConfig = INHERITABLE_CONTEXT.get();
			for(final String nextPropName: confMap.keySet()) {
				nextPropValue = nextPropName.startsWith("remote") ?
					localRunTimeConfig.getString(nextPropName) :
					confMap.get(nextPropName);
				LOG.trace(Markers.MSG, "Read property: \"{}\" = \"{}\"", nextPropName, nextPropValue);
				if(List.class.isInstance(nextPropValue)) {
					setProperty(
						nextPropName,
						StringUtils.join(List.class.cast(nextPropValue), LIST_SEP)
					);
				} else if(String.class.isInstance(nextPropValue)) {
					setProperty(nextPropName, String.class.cast(nextPropValue));
				} else if(nextPropValue==null) {
					LOG.warn(Markers.ERR, "Property \"{}\" is null", nextPropName);
				} else {
					LOG.error(
						Markers.ERR, "Unexpected type \"{}\" for property \"{}\"",
						nextPropValue.getClass().getCanonicalName(), nextPropName
					);
				}
			}
			INHERITABLE_CONTEXT.set(this);
			LOG.info(Markers.MSG, toString());
		} else {
			final String errMsg = String.format(
				"%s, version mismatch, server: %s client: %s",
				getRunName(), serverVersion, clientVersion
			);
			LOG.fatal(Markers.ERR, errMsg);
			throw new IOException(errMsg);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public synchronized void loadPropsFromDir(final Path propsDir) {
		DirectoryLoader.loadPropsFromDir(propsDir, this);
	}
	//
	public synchronized void loadSysProps() {
		final SystemConfiguration sysProps = new SystemConfiguration();
		String key, keys2override[];
		Object sharedValue;
		for(final Iterator<String> keyIter=sysProps.getKeys(); keyIter.hasNext();) {
			key = keyIter.next();
			LOG.trace(
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
	@Override
	public synchronized RunTimeConfig clone() {
		final RunTimeConfig runTimeConfig = RunTimeConfig.class.cast(super.clone());
		if(runTimeConfig != null) {
			runTimeConfig.set(
				KEY_RUN_ID,
				Main.FMT_DT.format(Calendar.getInstance(Main.TZ_UTC, Main.LOCALE_DEFAULT).getTime())
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
				case RunTimeConfig.KEY_RUN_SCENARIO_NAME:
				case RunTimeConfig.KEY_RUN_TIME:
				case RunTimeConfig.KEY_RUN_VERSION:
				case RunTimeConfig.KEY_DATA_COUNT:
				case RunTimeConfig.KEY_DATA_SIZE:
				case RunTimeConfig.KEY_DATA_RING_SEED:
				case RunTimeConfig.KEY_DATA_RING_SIZE:
				case RunTimeConfig.KEY_LOAD_THREADS:
				case RunTimeConfig.KEY_LOAD_TIME:
				case RunTimeConfig.KEY_STORAGE_ADDRS:
				case RunTimeConfig.KEY_STORAGE_API:
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
