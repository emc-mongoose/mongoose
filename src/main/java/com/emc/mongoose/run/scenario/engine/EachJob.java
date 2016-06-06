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
import java.util.Map;

import static com.emc.mongoose.common.io.value.PatternDefinedInput.FORMAT_CHARS;
import static com.emc.mongoose.common.io.value.PatternDefinedInput.PATTERN_CHAR;
/**
 Created by andrey on 04.06.16.
 */
public final class EachJob
extends SequentialJob {

	private final static Logger LOG = LogManager.getLogger();
	public final static String KEY_NODE_IN = "in";

	private final String replacePattern;
	private final List valueSeq;

	public EachJob(final AppConfig appConfig, final Map<String, Object> subTree) {
		super(appConfig, subTree);
		this.replacePattern = Character.toString(PATTERN_CHAR) + FORMAT_CHARS[0] +
			subTree.get(KEY_NODE_VALUE) + FORMAT_CHARS[1];
		this.valueSeq = (List) subTree.get(KEY_NODE_IN);
		loadSubTree(subTree);
	}

	@Override
	protected void appendNewJob(final Map<String, Object> subTree, final AppConfig config) {
		if(valueSeq != null) {
			AppConfig childJobConfig;
			try {
				for(final Object nextValue : valueSeq) {
					childJobConfig = (AppConfig) config.clone();
					findAndSubstitute(childJobConfig, replacePattern, nextValue);
					super.appendNewJob(subTree, childJobConfig);
				}
			} catch(final CloneNotSupportedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
			}
		}
	}

	private static void findAndSubstitute(
		final AppConfig appConfig, final String replacePattern, final Object nextReplaceValue
	) {
		final Iterator<String> keyIter = appConfig.getKeys();
		Object oldValue;
		String key;
		while(keyIter.hasNext()) {
			key = keyIter.next();
			oldValue = appConfig.getProperty(key);
			if(nextReplaceValue == null) {
				findAndSubstituteWithNull(appConfig, key, oldValue, replacePattern);
			} else {
				if(nextReplaceValue instanceof String) {
					findAndSubstituteWithString(
						appConfig, key, oldValue, replacePattern, (String) nextReplaceValue
					);
				} else if(nextReplaceValue instanceof Integer) {
					findAndSubstituteWithInteger(
						appConfig, key, oldValue, replacePattern, (int) nextReplaceValue
					);
				} else if(nextReplaceValue instanceof Long) {
					findAndSubstituteWithLong(
						appConfig, key, oldValue, replacePattern, (long) nextReplaceValue
					);
				} else if(nextReplaceValue instanceof Double) {
					findAndSubstituteWithDouble(
						appConfig, key, oldValue, replacePattern, (double) nextReplaceValue
					);
				} else if(nextReplaceValue instanceof Boolean) {
					findAndSubstituteWithBoolean(
						appConfig, key, oldValue, replacePattern, (boolean) nextReplaceValue
					);
				} else if(nextReplaceValue instanceof List) {
					findAndSubstituteWithList(
						appConfig, key, oldValue, replacePattern, (List) nextReplaceValue
					);
				} else {
					LOG.warn(Markers.ERR, "Unsupported value type for substitution: {}",
						nextReplaceValue.getClass()
					);
				}
			}
		}
	}

	private static void findAndSubstituteWithNull(
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

	private static void findAndSubstituteWithString(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final String newValue
	) {
		String t;
		if(oldValue instanceof String) {
			t = (String) oldValue;
			if(t.contains(pattern)) {
				appConfig.setProperty(key, t.replace(pattern, newValue));
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					t = (String) oldValueElement;
					if(t.contains(pattern)) {
						newValueList.add(t.replace(pattern, newValue));
					} else {
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

	private static void findAndSubstituteWithInteger(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final int newValue
	) {
		String t;
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				t = (String) oldValue;
				if(t.contains(pattern)) {
					appConfig.setProperty(key, t.replace(pattern, Integer.toString(newValue)));
				}
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						t = (String)oldValueElement;
						if(t.contains(pattern)) {
							newValueList.add(t.replace(pattern, Integer.toString(newValue)));
						} else {
							newValueList.add(oldValueElement);
						}
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private static void findAndSubstituteWithLong(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final long newValue
	) {
		String t;
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				t = (String) oldValue;
				if(t.contains(pattern)) {
					appConfig.setProperty(key, t.replace(pattern, Long.toString(newValue)));
				}
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						t = (String)oldValueElement;
						if(t.contains(pattern)) {
							newValueList.add(t.replace(pattern, Long.toString(newValue)));
						} else {
							newValueList.add(oldValueElement);
						}
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private static void findAndSubstituteWithDouble(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final double newValue
	) {
		String t;
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				t = (String) oldValue;
				if(t.contains(pattern)) {
					appConfig.setProperty(key, t.replace(pattern, Double.toString(newValue)));
				}
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						t = (String)oldValueElement;
						if(t.contains(pattern)) {
							newValueList.add(t.replace(pattern, Double.toString(newValue)));
						} else {
							newValueList.add(oldValueElement);
						}
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private static void findAndSubstituteWithBoolean(
		final AppConfig appConfig, final String key, final Object oldValue, final String pattern,
		final boolean newValue
	) {
		String t;
		if(oldValue instanceof String) {
			if(oldValue.equals(pattern)) {
				appConfig.setProperty(key, newValue);
			} else {
				t = (String) oldValue;
				if(t.contains(pattern)) {
					appConfig.setProperty(key, t.replace(pattern, Boolean.toString(newValue)));
				}
			}
		} else if(oldValue instanceof List) {
			final List<Object> newValueList = new ArrayList<>();
			for(final Object oldValueElement : (List) oldValue) {
				if(oldValueElement instanceof String) {
					if(oldValueElement.equals(pattern)) {
						newValueList.add(newValue);
					} else {
						t = (String)oldValueElement;
						if(t.contains(pattern)) {
							newValueList.add(t.replace(pattern, Boolean.toString(newValue)));
						} else {
							newValueList.add(oldValueElement);
						}
					}
				} else {
					newValueList.add(oldValueElement);
				}
			}
			appConfig.clearProperty(key);
			appConfig.setProperty(key, newValueList);
		}
	}

	private static void findAndSubstituteWithList(
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
	public final String toString() {
		return "eachJob#" + hashCode();
	}
}
