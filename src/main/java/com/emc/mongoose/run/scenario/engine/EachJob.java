package com.emc.mongoose.run.scenario.engine;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.load.model.metrics.IoStats;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
		// calls "appendNewJob", invoke it manually again after "valueSeq" is initialized already
		loadSubTree(subTree);
	}

	@Override
	protected void loadSubTree(final Map<String, Object> subTree) {
		if(valueSeq == null) {
			// skip the early invocation from the base class constructor while "valueSeq" is not
			// initialized yet
			return;
		}

		final Object nodeConfig = subTree.get(KEY_NODE_CONFIG);
		if(nodeConfig != null) {
			if(nodeConfig instanceof Map) {
				localConfig.override(null, (Map<String, Object>) nodeConfig);
			} else {
				LOG.error(Markers.ERR, "Invalid config node type: {}", nodeConfig.getClass());
			}
		}

		final Object jobTreeList = subTree.get(KEY_NODE_JOBS);
		try {
			if(jobTreeList != null) {
				if(jobTreeList instanceof List) {
					AppConfig childJobConfig;
					Map<String, Object> newJobTree;
					for(final Object nextValue : valueSeq) {
						childJobConfig = (AppConfig) localConfig.clone();
						childJobConfig.findAndSubstitute(replacePattern, nextValue);
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
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the configuration");
		}
	}

	private Map<String, Object> findAndSubstitute(
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

	private Map<String, Object> findAndSubstituteWithNull(
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

	private Map<String, Object> findAndSubstituteWithList(
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

	private <T> Map<String, Object> findAndSubstituteWith(
		final Map<String, Object> srcTree, final String replacePattern, final T newValue
	) {
		final Map<String, Object> dstTree = new LinkedHashMap<>();
		Object treeNode;
		String t;
		for(final String key : srcTree.keySet()) {
			treeNode = srcTree.get(key);
			if(treeNode instanceof Map) {
				dstTree.put(
					key,
					findAndSubstituteWith((Map<String, Object>) treeNode, replacePattern, newValue)
				);
			} else if(treeNode instanceof String) {
				if(treeNode.equals(replacePattern)) {
					dstTree.put(key, newValue);
				} else {
					t = (String) treeNode;
					if(t.contains(replacePattern)) {
						dstTree.put(key, t.replace(replacePattern, newValue.toString()));
					} else {
						dstTree.put(key, treeNode);
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
				dstTree.put(key, dstListNode);
			} else {
				dstTree.put(key, treeNode);
			}
		}
		return dstTree;
	}

	@Override
	public final String toString() {
		return "eachJob#" + hashCode();
	}

	@Override
	public final void close()
	throws IOException {
		try {
			final int valuesCount = valueSeq.size();
			if(valuesCount > 0) {
				final int k = childJobs.size() / valuesCount;
				if(childJobs.size() != valuesCount * k) {
					throw new IllegalStateException("");
				}
				Object nextValue;
				Job nextJob;
				IoStats.Snapshot nextStats;
				for(int i = 0; i < valuesCount; i++) {
					nextValue = valueSeq.get(i);
					LOG.info(Markers.MSG, "\t{}: {}", replacePattern, nextValue);
					for(int j = 0; j < k; j++) {
						nextJob = childJobs.get(j * valuesCount + i);
						if(nextJob instanceof LoadJob) {
							nextStats = ((LoadJob) nextJob).getLastStats();
							if(nextStats != null) {
								LOG.info(Markers.MSG, nextStats.toSummaryString());
							} else {
								LOG.warn(Markers.ERR, "Null stats for the load job \"{}\"", nextJob);
							}
						}
						try {
							nextJob.close();
						} catch(final IOException e) {
							LogUtil.exception(
								LOG, Level.WARN, e, "Failed to close the job \"{}\"", nextJob
							);
						}
					}
				}
				valueSeq.clear();
			}
		} finally {
			super.close();
		}
	}
}
