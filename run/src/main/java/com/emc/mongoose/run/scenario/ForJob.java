package com.emc.mongoose.run.scenario;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import com.emc.mongoose.ui.log.Markers;
import static com.emc.mongoose.common.supply.PatternDefinedSupplier.FORMAT_CHARS;

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

	public ForJob(final Config appConfig, final Map<String, Object> subTree)
	throws ScenarioParseException {
		super(appConfig, subTree);
		final Object value = subTree.get(KEY_NODE_VALUE);
		if(value != null) {
			infinite = false;
			if(value instanceof Short || value instanceof Integer) {
				final int n = (int) value;
				replaceMarkerName = null;
				valueSeq = new ArrayList(n);
				for(int i = 0; i < n; i ++) {
					valueSeq.add(i);
				}
			} else if(value instanceof String) {
				final Object seqSpec = subTree.get(KEY_NODE_IN);
				if(seqSpec == null) {
					// sequence is a number surrounded with quotes?
					try {
						final int n = Integer.parseInt((String) value);
						replaceMarkerName = null;
						valueSeq = new ArrayList(n);
						for(int i = 0; i < n; i ++) {
							valueSeq.add(i);
						}
					} catch(final NumberFormatException e) {
						throw new ScenarioParseException(
							"Expected an integer value for the \"value\", but got: \"" + value +
							"\""
						);
					}
				} else {
					this.replaceMarkerName = (String) value;
					if(seqSpec instanceof List) {
						// sequence is the json array
						this.valueSeq = (List) seqSpec;
					} else if(seqSpec instanceof String) {
						// sequence is a replacement placeholder
						final Matcher m = SEQ_SPEC_PATTERN.matcher((String) seqSpec);
						if(!m.matches()) {
							throw new ScenarioParseException(
								"String \"in\" value \"" + seqSpec +
								"\" should match the pattern: \"" + SEQ_SPEC_PATTERN.pattern() +
								"\""
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
								throw new ScenarioParseException("No start value in the \"in\"");
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
									throw new ScenarioParseException("Step value should be > 0");
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
							throw new ScenarioParseException(
								"String \"in\" value \"" + seqSpec +
								"\" should match the pattern: \"" +
								SEQ_SPEC_PATTERN.pattern() + "\""
							);
						}
					} else {
						throw new ScenarioParseException(
							"Unexpected \"in\" value: \"" + seqSpec + "\""
						);
					}
				}
			} else {
				throw new ScenarioParseException("Unexpected value: \"" + value + "\"");
			}
		} else {
			infinite = true;
			valueSeq = null;
			replaceMarkerName = null;
		}
		// calls "appendNewJob", invoking it manually again after "valueSeq" is initialized already
		loadSubTree(subTree);
	}

	@Override
	protected void loadSubTree(final Map<String, Object> subTree)
	throws ScenarioParseException {

		if(infinite) {
			super.loadSubTree(subTree);
			return;
		}

		if(valueSeq == null) {
			// skip the early invocation from the base class constructor while "valueSeq" is not
			// initialized yet
			return;
		} else if(replaceMarkerName == null) {
			for(final Object o : valueSeq) {
				super.loadSubTree(subTree);
			}
			return;
		}

		final Object nodeConfig = subTree.get(KEY_NODE_CONFIG);
		/*if(nodeConfig != null) {
			if(nodeConfig instanceof Map) {
				localConfig.apply((Map<String, Object>) nodeConfig);
			} else {
				throw new ScenarioParseException(
					"Invalid config node type: \"" + nodeConfig.getClass() + "\""
				);
			}
		}*/

		final Object jobTreeList = subTree.get(KEY_NODE_JOBS);
		final String replacePattern = Character.toString(REPLACE_MARKER_CHAR) + FORMAT_CHARS[0] +
			replaceMarkerName + FORMAT_CHARS[1];
		Map<String, Object> nextNodeConfig;
		try {
			if(jobTreeList != null) {
				if(jobTreeList instanceof List) {
					Config childJobConfig;
					Map<String, Object> newJobTree;
					for(final Object nextValue : valueSeq) {
						childJobConfig = ConfigParser.replace(
							localConfig, replacePattern, nextValue
						);
						if(nodeConfig != null) {
							nextNodeConfig = ConfigParser.replace(
								(Map<String, Object>) nodeConfig, replacePattern, nextValue
							);
							childJobConfig.apply(nextNodeConfig);
						}
						append(
							new BasicTaskJob(
								() -> LOG.info(
									Markers.MSG, "Use next value for \"{}\": {}", replaceMarkerName,
									nextValue
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
									throw new ScenarioParseException(
										"Invalid job node type: \"" + job.getClass() + "\""
									);
								}
							} else {
								throw new ScenarioParseException("job node is null");
							}
						}
					}
				} else {
					throw new ScenarioParseException(
						"Invalid jobs node type: \"" + jobTreeList.getClass() + "\""
					);
				}
			}
		} catch(final UserShootHisFootException | IOException e) {
			throw new ScenarioParseException("Failed to replace the configuration values", e);
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
		if(replaceMarkerName == null && valueSeq == null) { // infinite loop
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
