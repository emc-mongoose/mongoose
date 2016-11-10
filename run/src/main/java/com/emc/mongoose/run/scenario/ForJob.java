package com.emc.mongoose.run.scenario;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;
import static com.emc.mongoose.common.io.pattern.PatternDefinedInput.FORMAT_CHARS;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 Created by andrey on 04.06.16.
 */
public final class ForJob
extends SequentialJob {

	private static final Logger LOG = LogManager.getLogger();
	
	public static final String KEY_NODE_IN = "in";
	public static final char REPLACE_MARKER_CHAR = '$';
	public static final Pattern SEQ_SPEC_PATTERN = Pattern.compile(
		"(-?[\\d.]+)(-(-?[\\d.]+)(,([\\d.]+))?)?"
	);

	private final String replaceMarkerName;
	private final List valueSeq;
	private final boolean infinite;

	public ForJob(final Config appConfig, final Map<String, Object> subTree) {
		super(appConfig, subTree);
		final Object value = subTree.get(KEY_NODE_VALUE);
		if(value != null) {
			if(value instanceof String) {
				this.replaceMarkerName = (String) subTree.get(KEY_NODE_VALUE);
				final Object seqSpec = subTree.get(KEY_NODE_IN);
				if(seqSpec instanceof List) {
					this.valueSeq = (List) seqSpec;
				} else if(seqSpec instanceof Short) {
					final short n = (short) seqSpec;
					valueSeq = new ArrayList(n);
					for(short i = 0; i < n; i ++) {
						valueSeq.add(i);
					}
				} else if(seqSpec instanceof Integer) {
					final int n = (int) seqSpec;
					valueSeq = new ArrayList(n);
					for(int i = 0; i < n; i ++) {
						valueSeq.add(i);
					}
				} else if(seqSpec instanceof String) {
					final Matcher m = SEQ_SPEC_PATTERN.matcher((String) seqSpec);
					if(!m.matches()) {
						throw new IllegalArgumentException(
							"String \"in\" value \"" + seqSpec + "\" should match the pattern: \"" +
							SEQ_SPEC_PATTERN.pattern() + "\""
						);
					}
					m.reset();
					if(m.find()) {
						valueSeq = new ArrayList();
						double start;
						double end = Double.NaN;
						double step = Double.NaN;
						String t = m.group(1);
						if(t != null) {
							start = Double.parseDouble(t);
						} else {
							throw new IllegalArgumentException("No start value in the \"in\"");
						}
						t = m.group(3);
						if(t != null) {
							end = Double.parseDouble(t);
						}
						t = m.group(5);
						if(t != null) {
							step = Double.parseDouble(t);
						}
						if(Double.isNaN(step)) {
							step = 1;
						}
						if(Double.isNaN(end)) {
							end = start;
							start = 0;
						}
						if(start == end) {
							if(start == (long) start) {
								valueSeq.add((long) start);
							} else {
								valueSeq.add(start);
							}
						} else {
							if(step <= 0) {
								throw new IllegalArgumentException("Step value should be > 0");
							}
							if(start < end) {
								LOG.info(
									Markers.MSG, "Parsed loop range: {} = {}, {} <= {}, {} += {}",
									replaceMarkerName, start, replaceMarkerName, end,
									replaceMarkerName, step
								);
								for(double i = start; i <= end; i += step) {
									if(i == (long) i) {
										valueSeq.add((long) i);
									} else {
										valueSeq.add(i);
									}
								}
							} else {
								LOG.info(
									Markers.MSG, "Parsed loop range: {} = {}, {} => {}, {} -= {}",
									replaceMarkerName, start, replaceMarkerName, end,
									replaceMarkerName, step
								);
								for(double i = start; i >= end; i -= step) {
									if(i == (long) i) {
										valueSeq.add((long) i);
									} else {
										valueSeq.add(i);
									}
								}
							}
						}
					} else {
						throw new IllegalArgumentException(
							"String \"in\" value \"" + seqSpec + "\" should match the pattern: \"" +
							SEQ_SPEC_PATTERN.pattern() + "\""
						);
					}
				} else {
					throw new IllegalArgumentException(
						"Unexpected \"in\" value: \"" + seqSpec + "\""
					);
				}
			} else {
				throw new IllegalArgumentException("Unexpected value: \"" + value + "\"");
			}
			infinite = false;
		} else {
			valueSeq = null;
			replaceMarkerName = null;
			infinite = true;
		}
		// calls "appendNewJob", invoking it manually again after "valueSeq" is initialized already
		loadSubTree(subTree);
	}

	@Override
	protected void loadSubTree(final Map<String, Object> subTree) {

		if(infinite) {
			// no values substitution is expected
			super.loadSubTree(subTree);
			return;
		}

		if(valueSeq == null) {
			// skip the early invocation from the base class constructor while "valueSeq" is not
			// initialized yet
			return;
		}

		final Object nodeConfig = subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			if(nodeConfig instanceof Map) {
				localConfig.apply((Map<String, Object>) nodeConfig);
			} else {
				LOG.error(Markers.ERR, "Invalid config node type: {}", nodeConfig.getClass());
			}
		}

		final Object jobTreeList = subTree.get(KEY_NODE_JOBS);
		final String replacePattern = Character.toString(REPLACE_MARKER_CHAR) +
			FORMAT_CHARS[0] + replaceMarkerName + FORMAT_CHARS[1];
		try {
			if(jobTreeList != null) {
				if(jobTreeList instanceof List) {
					Config childJobConfig;
					Map<String, Object> newJobTree;
					for(final Object nextValue : valueSeq) {
						childJobConfig = ConfigParser.replace(
							localConfig, replacePattern, nextValue
						);
						append(
							new BasicTaskJob(
								() -> LOG.info(
									Markers.MSG, "Use next value for \"{}\": {}",
									replaceMarkerName, nextValue
								)
							)
						);
						for(final Object job : (List) jobTreeList) {
							if(job != null) {
								if(job instanceof Map) {
									newJobTree = findAndSubstitute(
										(Map<String, Object>) job, replacePattern, nextValue
									);
									appendNewJob(newJobTree, childJobConfig);
								} else {
									LOG.error(
										Markers.ERR, "Invalid job node type: {}", job.getClass());
								}
							} else {
								LOG.warn(Markers.ERR, "{}: job node is null");
							}
						}
					}
				} else {
					LOG.error(Markers.ERR, "Invalid jobs node type: {}", jobTreeList.getClass());
				}
			}
		} catch(final UserShootHisFootException | IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to replace the configuration values");
		}
	}

	private static Map<String, Object> findAndSubstitute(
		final Map<String, Object> srcTree, final String replacePattern, final Object newValue
	) {
		if(newValue == null) {
			return findAndSubstituteWithNull(srcTree, replacePattern);
		} else if(newValue instanceof List) {
			return findAndSubstituteWithList(srcTree, replacePattern, (List) newValue);
		} else {
			return findAndSubstituteWith(srcTree, replacePattern, newValue);
		}
	}

	private static Map<String, Object> findAndSubstituteWithNull(
		final Map<String, Object> srcTree, final String replacePattern
	) {
		final Map<String, Object> dstTree = new LinkedHashMap<>();
		Object treeNode;
		for(final String key : srcTree.keySet()) {
			treeNode = srcTree.get(key);
			if(treeNode instanceof Map) {
				dstTree.put(
					key, findAndSubstituteWithNull((Map<String, Object>) treeNode, replacePattern)
				);
			} else if(treeNode instanceof String) {
				if(treeNode.equals(replacePattern)) {
					dstTree.put(key, null);
				} else {
					dstTree.put(key, treeNode);
				}
			} else if(treeNode instanceof List) {
				final List srcListNode = (List) treeNode;
				final List<Object> dstListNode = new ArrayList<>(srcListNode.size());
				for(final Object element : srcListNode) {
					if(element instanceof Map) {
						dstListNode.add(
							findAndSubstituteWithNull((Map<String, Object>) element, replacePattern)
						);
					} else if(element instanceof String) {
						if(element.equals(replacePattern)) {
							dstListNode.add(null);
						} else {
							dstListNode.add(element);
						}
					} else {
						dstListNode.add(element);
					}
				}
				dstTree.put(key, dstListNode);
			} else {
				dstTree.put(key, treeNode);
			}
		}
		return dstTree;
	}

	private static Map<String, Object> findAndSubstituteWithList(
		final Map<String, Object> srcTree, final String replacePattern, final List newValue
	) {
		final Map<String, Object> dstTree = new LinkedHashMap<>();
		Object treeNode;
		for(final String key : srcTree.keySet()) {
			treeNode = srcTree.get(key);
			if(treeNode instanceof Map) {
				dstTree.put(
					key,
					findAndSubstituteWithList(
						(Map<String, Object>) treeNode, replacePattern, newValue
					)
				);
			} else if(treeNode instanceof String) {
				if(treeNode.equals(replacePattern)) {
					dstTree.put(key, newValue);
				} else {
					dstTree.put(key, treeNode);
				}
			} else if(treeNode instanceof List) {
				final List srcListNode = (List) treeNode;
				final List<Object> dstListNode = new ArrayList<>(srcListNode.size());
				for(final Object element : srcListNode) {
					if(element instanceof Map) {
						dstListNode.add(
							findAndSubstituteWithList(
								(Map<String, Object>) element, replacePattern, newValue
							)
						);
					} else if(element instanceof String) {
						if(element.equals(replacePattern)) {
							dstListNode.add(newValue);
						} else {
							dstListNode.add(element);
						}
					} else {
						dstListNode.add(element);
					}
				}
				dstTree.put(key, dstListNode);
			} else {
				dstTree.put(key, treeNode);
			}
		}
		return dstTree;
	}

	private static <T> Map<String, Object> findAndSubstituteWith(
		final Map<String, Object> srcTree, final String replacePattern, final T newValue
	) {
		final Map<String, Object> dstTree = new LinkedHashMap<>();
		Object treeNode;
		String t, newKey;
		for(final String key : srcTree.keySet()) {

			if(key.contains(replacePattern)) {
				newKey = key.replace(replacePattern, newValue.toString());
			} else {
				newKey = key;
			}

			treeNode = srcTree.get(key);
			if(treeNode instanceof Map) {
				dstTree.put(
					newKey,
					findAndSubstituteWith((Map<String, Object>) treeNode, replacePattern, newValue)
				);
			} else if(treeNode instanceof String) {
				if(treeNode.equals(replacePattern)) {
					dstTree.put(newKey, newValue);
				} else {
					t = (String) treeNode;
					if(t.contains(replacePattern)) {
						dstTree.put(newKey, t.replace(replacePattern, newValue.toString()));
					} else {
						dstTree.put(newKey, treeNode);
					}
				}
			} else if(treeNode instanceof List) {
				final List srcListNode = (List) treeNode;
				final List<Object> dstListNode = new ArrayList<>(srcListNode.size());
				for(final Object element : srcListNode) {
					if(element instanceof Map) {
						dstListNode.add(
							findAndSubstituteWith(
								(Map<String, Object>) element, replacePattern, newValue
							)
						);
					} else if(element instanceof String) {
						if(element.equals(replacePattern)) {
							dstListNode.add(newValue);
						} else {
							t = (String) element;
							if(t.contains(replacePattern)) {
								dstListNode.add(t.replace(replacePattern, newValue.toString()));
							} else {
								dstListNode.add(element);
							}
						}
					} else {
						dstListNode.add(element);
					}
				}
				dstTree.put(newKey, dstListNode);
			} else {
				dstTree.put(newKey, treeNode);
			}
		}
		return dstTree;
	}

	@Override
	public final void run() {
		if(replaceMarkerName == null) {
			while(true) {
				super.run();
			}
		} else {
			super.run();
		}
	}

	@Override
	public final String toString() {
		return "forJob" + (replaceMarkerName == null ? "Infinite" : valueSeq.size()) + "#" +
			hashCode();
	}

	@Override
	public final void close()
	throws IOException {
		try {
			super.close();
		} finally {
			if(valueSeq != null) {
				valueSeq.clear();
			}
		}
	}
}
