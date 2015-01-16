package com.emc.mongoose.util.conf;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.keyvalue.DefaultMapEntry;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
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
	private Set<String> mongooseKeys;
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String LIST_SEP = ",", KEY_VERSION = "run.version";
	private final static Map<String, String[]> MAP_OVERRIDE = new HashMap<>();
	//
	public final Map<String, Object> properties = new HashMap<>();
	//
	private final static DateFormat FMT_DT = new SimpleDateFormat(
			"yyyy.MM.dd.HH.mm.ss.SSS", Locale.ROOT
	);
	static {
		MAP_OVERRIDE.put("data.size", new String[] {"data.size.min", "data.size.max"});
		MAP_OVERRIDE.put("load.step.time", new String[] { "run.time" });
		MAP_OVERRIDE.put("load.threads", new String[] {"load.append.threads", "load.create.threads", "load.read.threads", "load.update.threads", "load.delete.threads"});
		MAP_OVERRIDE.put("remote.drivers", new String[] {"remote.servers"});
	}
	//
	private final static String
		SIZE_UNITS = "kmgtpe",
		FMT_MSG_INVALID_SIZE = "The string \"%s\" doesn't match the pattern: \"%s\"";
	private final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)b?");
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
		LOG.trace(Markers.MSG, "\"{}\" is {} bytes", value, size);
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
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed json processing");
		}
		return null;
	}
	//
	public final synchronized void put(final List<String> dirs, final String fileName, final List<DefaultMapEntry<String, Object>> props) {
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
	public final synchronized void set(final String key, final String value) {
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
		return getString("storage.api");
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
	public final int getRunMetricsPeriodSec() {
		return getInt("run.metrics.period.sec");
	}
	//
	public final int getRunRequestQueueFactor() {
		return getInt("run.request.queue.factor");
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
	public final String getDataNameSpace() {
		return getString("data.namespace");
	}
	//
	public final String getHttpSignMethod() {
		return getString("http.sign.method");
	}
	//
	public final String getRunName() {
		return getString("run.name");
	}
	//
	public final String getRunVersion() {
		return getString(KEY_VERSION);
	}
	//
	public final long getDataCount() {
		return getLong("data.count");
	}
	//
	public final String[] getStorageAddrs() {
		return getStringArray("storage.addrs");
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
	public final boolean getStaleConnCheckFlag() {
		return getBoolean("storage.connection.stale.check");
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
		return getString("run.scenario.name");
	}
	//
	public final String getRunScenarioDir() {
		return getString("run.scenario.dir");
	}
	//
	public final String getRunId() {
		return getString(Main.KEY_RUN_ID);
	}
	//
	public final String getRunTime() {
		return getString("run.time");
	}
	//
	public final String getRunMode() {
		return getString(Main.KEY_RUN_MODE);
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
			} else if(nextPropValue==null) {
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
			serverVersion = Main.RUN_TIME_CONFIG.get().getRunVersion(),
			clientVersion = confMap.get(KEY_VERSION);
		if(serverVersion.equals(clientVersion)) {
			// put the properties into the System
			Object nextPropValue;
			final RunTimeConfig localRunTimeConfig = Main.RUN_TIME_CONFIG.get();
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
			if(keys2override==null) {
				setProperty(key, sharedValue);
			} else {
				for(final String key2override: keys2override) {
					setProperty(key2override, sharedValue);
				}
			}
		}
	}
	//
	public synchronized RunTimeConfig clone() {
		final RunTimeConfig runTimeConfig = RunTimeConfig.class.cast(super.clone());
		runTimeConfig.set(Main.KEY_RUN_ID, FMT_DT.format(
				Calendar.getInstance(TimeZone.getTimeZone("GMT+0")).getTime()));
		return runTimeConfig;
	}

	public void overrideSystemProperties(Map<String, String> props){
		for(Map.Entry<String, String> entry : props.entrySet()){
			setProperty(entry.getKey(), entry.getValue());
		}
	}
}
