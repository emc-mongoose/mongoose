package com.emc.mongoose.run.scenario.engine;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.emc.mongoose.common.io.value.PatternDefinedInput.FORMAT_CHARS;
import static com.emc.mongoose.common.io.value.PatternDefinedInput.PATTERN_CHAR;
/**
 Created by andrey on 04.06.16.
 */
public final class EachJobContainer
implements JobContainer{

	private final static Logger LOG = LogManager.getLogger();

	private final List<AppConfig> appConfigSeq;

	public EachJobContainer(final AppConfig appConfig, final String pattern, final List seq) {
		appConfigSeq = new ArrayList<>(seq.size());
		final Iterator<String> keyIter = appConfig.getKeys();
		String key;
		Object nextConfigValue;
		while(keyIter.hasNext()) {
			key = keyIter.next();
			nextConfigValue = appConfig.getProperty(key);
			findAndSubstituteEach(
				appConfig, key, nextConfigValue,
				Character.toString(PATTERN_CHAR) + FORMAT_CHARS[0] + pattern + FORMAT_CHARS[1], seq
			);
		}
	}

	private void findAndSubstituteEach(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final List seq
	) {
		AppConfig nextConfig;
		try {
			for(final Object nextValue : seq) {
				nextConfig = (AppConfig) appConfig.clone();
				appConfigSeq.add(nextConfig);
				if(nextValue == null) {
					findAndSubstituteWithNull(nextConfig, key, oldValue, pattern);
				} else {
					if(nextValue instanceof String) {
						findAndSubstituteWithString(
							nextConfig, key, oldValue, pattern, (String) nextValue
						);
					} else if(nextValue instanceof Integer) {
						findAndSubstituteWithInteger(
							nextConfig, key, oldValue, pattern, (int) nextValue
						);
					} else if(nextValue instanceof Long) {
						findAndSubstituteWithLong(
							nextConfig, key, oldValue, pattern, (long) nextValue
						);
					} else if(nextValue instanceof Double) {
						findAndSubstituteWithDouble(
							nextConfig, key, oldValue, pattern, (double) nextValue
						);
					} else if(nextValue instanceof Boolean) {
						findAndSubstituteWithBoolean(
							nextConfig, key, oldValue, pattern, (boolean) nextValue
						);
					} else if(nextValue instanceof List) {
						findAndSubstituteWithList(
							nextConfig, key, oldValue, pattern, (List) nextValue
						);
					} else {
						LOG.warn(Markers.ERR, "Unsupported value type for substitution: {}",
							nextValue.getClass()
						);
					}
				}
			}
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
		}
	}

	private void findAndSubstituteWithNull(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, null);
			} else {
				LOG.warn(Markers.ERR, "Couldn't replace with null value(s) the string part");
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValue = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement.equals(pattern)) {
					newValue.add(null);
				} else {
					LOG.warn(Markers.ERR, "Couldn't replace with null value(s) the string part");
					newValue.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValue);
		}
	}

	private void findAndSubstituteWithString(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final String newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(key)) {
				appConfig.setProperty(key, ((String) oldValue).replace(pattern, newValue));
			} else {
				LOG.warn(Markers.ERR, "Couldn't replace with null value(s) the string part");
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					newValueList.add(((String) oldValueElement).replace(pattern, newValue));
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private void findAndSubstituteWithInteger(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final int newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				appConfig.setProperty(
					key, ((String) oldValue).replace(pattern, Integer.toString(newValue))
				);
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						newValueList.add(
							((String) oldValueElement).replace(pattern, Integer.toString(newValue))
						);
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private void findAndSubstituteWithLong(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final long newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				appConfig.setProperty(
					key, ((String) oldValue).replace(pattern, Long.toString(newValue))
				);
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						newValueList.add(
							((String) oldValueElement).replace(pattern, Long.toString(newValue))
						);
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private void findAndSubstituteWithDouble(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final double newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				appConfig.setProperty(
					key, ((String) oldValue).replace(pattern, Double.toString(newValue))
				);
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						newValueList.add(
							((String) oldValueElement).replace(pattern, Double.toString(newValue))
						);
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private void findAndSubstituteWithBoolean(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final boolean newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				appConfig.setProperty(
					key, ((String) oldValue).replace(pattern, Boolean.toString(newValue))
				);
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						newValueList.add(
							((String) oldValueElement).replace(pattern, Boolean.toString(newValue))
						);
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private void findAndSubstituteWithList(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final List newValue
	) {
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				LOG.warn(Markers.ERR, "Couldn't replace with list value(s) the string part");
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						LOG.warn(
							Markers.ERR, "Couldn't replace with list value(s) the string part"
						);
						newValueList.add(oldValueElement);
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	@Override
	public final AppConfig getConfig() {
		return null;
	}

	@Override
	public final boolean append(final JobContainer subJob) {
		return false;
	}

	@Override
	public final void run() {
	}

	@Override
	public final String toString() {
		return "eachJobContainer#" + hashCode();
	}

	@Override
	public final void close() {
		appConfigSeq.clear();
	}
}
