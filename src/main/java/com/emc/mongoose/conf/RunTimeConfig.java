package com.emc.mongoose.conf;
//
import com.emc.mongoose.logging.Markers;
import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.ConversionException;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 28.05.14.
 */
public final class RunTimeConfig
implements Externalizable {
	//
	private final static Logger LOG = LogManager.getLogger();
	public final static String LIST_SEP = ",", KEY_VERSION = "run.version";
	private final static Map<String, String[]> MAP_OVERRIDE = new HashMap<>();
	static {
		MAP_OVERRIDE.put(
			"data.size",
			new String[] {
				"data.size.min",
				"data.size.max"
			}
		);
		MAP_OVERRIDE.put(
			"load.threads",
			new String[] {
				"load.create.threads",
				"load.read.threads",
				"load.update.threads",
				"load.delete.threads"
			}
		);
	}
	private static BaseConfiguration INSTANCE = new BaseConfiguration();
	//
	public static boolean containsKey(final String key) {
		return INSTANCE.containsKey(key);
	}
	//
	public static BigInteger getBigInteger(final String key)
	throws NoSuchElementException {
		return INSTANCE.getBigInteger(key);
	}
	//
	public static boolean getBoolean(final String key)
	throws NoSuchElementException {
		return INSTANCE.getBoolean(key);
	}
	//
	public static byte getByte(final String key)
	throws NoSuchElementException {
		return INSTANCE.getByte(key);
	}
	//
	public static double getDouble(final String key)
	throws NoSuchElementException {
		return INSTANCE.getDouble(key);
	}
	//
	public static float getFloat(final String key)
	throws NoSuchElementException {
		return INSTANCE.getFloat(key);
	}
	//
	public static int getInt(final String key)
	throws NoSuchElementException {
		return INSTANCE.getInt(key);
	}
	//
	public static Iterator<String> getKeys() {
		return INSTANCE.getKeys();
	}
	//
	public static Iterator<String> getKeys(final String prefix) {
		return INSTANCE.getKeys(prefix);
	}
	//
	public static List<Object> getList(final String key)
	throws ConversionException, NoSuchElementException {
		return INSTANCE.getList(key);
	}
	//
	public static long getLong(final String key)
	throws NoSuchElementException {
		return INSTANCE.getLong(key);
	}
	//
	public static Object getProperty(final String key)
	throws NoSuchElementException {
		return INSTANCE.getProperty(key);
	}
	//
	public static short getShort(final String key)
	throws NoSuchElementException {
		return INSTANCE.getShort(key);
	}
	//
	public static String getString(final String key)
	throws NoSuchElementException {
		return INSTANCE.getString(key);
	}
	//
	public static String[] getStringArray(final String key)
	throws ConversionException, NoSuchElementException {
		return INSTANCE.getStringArray(key);
	}
	//
	private final static String SIZE_UNITS = "bkMGTPE";
	private final static Pattern PATTERN_SIZE = Pattern.compile("(\\d+)(["+SIZE_UNITS+"]?)i?b?");
	//
	public static long getSizeBytes(final String key) {
		final String value = INSTANCE.getString(key), unit;
		final Matcher matcher = PATTERN_SIZE.matcher(value);
		long size = -1;
		long degree = 0;
		if(matcher.matches() && matcher.groupCount() > 0 && matcher.groupCount() < 3) {
			size = Long.valueOf(matcher.group(1), 10);
			unit = matcher.group(2);
			if(unit.length()==1) {
				degree = SIZE_UNITS.indexOf(matcher.group(2));
			}
		} else {
			throw new IllegalArgumentException("The string \""+key+"\" doesn't match the pattern: \""+PATTERN_SIZE+"\"");
		}
		size *= 1L << 10 * degree;
		LOG.trace(Markers.MSG, "\"{}\" is {} bytes", value, size);
		return size;
	}
	//
	public static String formatSize(final long v) {
		if(v < 1024) {
			return v + "b";
		}
		final int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
		final double x = (double) v / (1L << (z*10));
		return String.format(
			Locale.ROOT,
			x < 10 ? "%.3f%sib" : x < 100 ? "%.2f%sib" : "%.1f%sib",
			x, SIZE_UNITS.charAt(z)
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final synchronized void writeExternal(final ObjectOutput out)
	throws IOException {
		LOG.debug(Markers.MSG, "Going to upload properties to a driver");
		String nextPropName;
		Object nextPropValue;
		final HashMap<String, String> propsMap = new HashMap<>();
		for(final Iterator<String> i = INSTANCE.getKeys(); i.hasNext();) {
			nextPropName = i.next();
			nextPropValue = INSTANCE.getProperty(nextPropName);
			LOG.trace(Markers.MSG, "Write property: \"{}\" = \"{}\"", nextPropName, nextPropValue);
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
		LOG.debug(Markers.MSG, "Uploaded the properties from controller side");
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final synchronized void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		LOG.debug(Markers.MSG, "Going to fetch the properties from controller side");
		final HashMap<String, String> propsMap = HashMap.class.cast(in.readObject());
		LOG.trace(Markers.MSG, "Got the properties from controller side: {}", propsMap);
		//
		final String
			driverVersion = getString(KEY_VERSION),
			controllerVersion = propsMap.get(KEY_VERSION);
		if(driverVersion.equals(controllerVersion)) {
			// put the properties into the System
			Object nextPropValue;
			for(final String nextPropName: propsMap.keySet()) {
				nextPropValue = propsMap.get(nextPropName);
				LOG.trace(Markers.MSG, "Read property: \"{}\" = \"{}\"", nextPropName, nextPropValue);
				if(List.class.isInstance(nextPropValue)) {
					INSTANCE.setProperty(
						nextPropName,
						StringUtils.join(List.class.cast(nextPropValue), LIST_SEP)
					);
				} else if(String.class.isInstance(nextPropValue)) {
					INSTANCE.setProperty(nextPropName, String.class.cast(nextPropValue));
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
			LOG.fatal(
				Markers.ERR, "Version mismatch, driver: {}, controller: {}",
				driverVersion, controllerVersion
			);
			throw new IOException("Version mismatch");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static void loadPropsFromDir(final Path propsDir) {
		DirectoryLoader.loadPropsFromDir(propsDir, INSTANCE);
	}
	//
	public static void loadSysProps() {
		final SystemConfiguration sysProps = new SystemConfiguration();
		String key, keys2override[];
		Object sharedValue;
		for(final Iterator<String> keyIter=sysProps.getKeys(); keyIter.hasNext();) {
			key = keyIter.next();
			LOG.trace(
				Markers.MSG, "System property: \"{}\": \"{}\" -> \"{}\"",
				key, INSTANCE.getProperty(key), sysProps.getProperty(key)
			);
			keys2override = MAP_OVERRIDE.get(key);
			sharedValue = sysProps.getProperty(key);
			if(keys2override==null) {
				INSTANCE.setProperty(key, sharedValue);
			} else {
				for(final String key2override: keys2override) {
					INSTANCE.setProperty(key2override, sharedValue);
				}
			}
		}
	}
	//
}
