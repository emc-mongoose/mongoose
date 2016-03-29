package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 02.02.16.
 */
public class JsonScenario
extends SequentialJobContainer
implements Scenario {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String KEY_NODE_JOBS = "jobs";
	private final static String KEY_TYPE = "type";
	private final static String KEY_CONFIG = "config";
	private final static String VALUE_TYPE_PARALLEL = "parallel";
	private final static String VALUE_TYPE_SEQUENTIAL = "sequential";
	private final static String VALUE_TYPE_LOAD = "load";
	private final static String VALUE_TYPE_RAMPUP = "rampup";
	//
	public JsonScenario(final File scenarioSrcFile) {
		final ObjectMapper jsonMapper = new ObjectMapper()
			.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
			.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);
		if(!scenarioSrcFile.exists()) {
			LOG.error(Markers.ERR, "Scenario file is not specified");
			return;
		}
		if(!scenarioSrcFile.isFile()) {
			LOG.error(Markers.ERR, "Not a valid scenario file: \"{}\"", scenarioSrcFile.toString());
			return;
		}
		//
		try {
			final Map<String, Object> tree = jsonMapper.readValue(scenarioSrcFile, new
					TypeReference<Map<String, Object>>(){});
			loadTree(tree, this);
		} catch(final IOException e) {
			LogUtil.exception(
				LOG, Level.ERROR, e, "Scenario \"{}\" initialization failure", scenarioSrcFile
			);
		}
	}
	//
	private static void loadTree(final Map<String, Object> node, final JobContainer jobContainer)
	throws IOException {
		LOG.debug(Markers.MSG, "Load the subtree to the container \"{}\"", jobContainer);
		Object value;
		JobContainer subContainer = jobContainer, newSubContainer;
		for(final String key : node.keySet()) {
			value = node.get(key);
			switch(key) {
				case KEY_NODE_JOBS:
					if(value instanceof Map) {
						LOG.warn(Markers.ERR, "{}: {}: map value: {}", jobContainer, key, value);
					} else if(value instanceof List) {
						for(final Object e : (List) value) {
							if(e instanceof Map) {
								loadTree((Map<String, Object>) e, subContainer);
							} else {
								LOG.warn(
									Markers.ERR, "Unexpected list element type: {}",
									value.getClass()
								);
							}
						}
					} else if(value instanceof Double) {
						LOG.warn(Markers.ERR, "{}: {}: double value: {}", jobContainer, key, value);
					} else if(value instanceof Integer) {
						LOG.warn(
							Markers.ERR, "{}: {}: integer value: {}", jobContainer, key, value
						);
					} else if(value instanceof Long) {
						LOG.warn(
							Markers.ERR, "{}: {}: long value: {}", jobContainer, key, value
						);
					} else if(value instanceof Boolean) {
						LOG.warn(
							Markers.ERR, "{}: {}: boolean value: {}", jobContainer, key, value
						);
					} else if(value instanceof String) {
						LOG.warn(Markers.ERR, "{}: {}: string value: {}", jobContainer, key, value);
					} else if(value == null) {
						LOG.warn(Markers.ERR, "{}: {}: null value: {}", jobContainer, key, value);
					} else {
						LOG.warn(
							Markers.ERR, "{}: unexpected value type: {}",
							jobContainer, value.getClass()
						);
					}
					break;
				case KEY_TYPE:
					if(value instanceof String) {
						switch((String) value) {
							case VALUE_TYPE_PARALLEL:
								newSubContainer = new ParallelJobContainer();
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case VALUE_TYPE_SEQUENTIAL:
								newSubContainer = new SequentialJobContainer();
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case VALUE_TYPE_LOAD:
							case VALUE_TYPE_RAMPUP:
								final Object configTree = node.get(KEY_CONFIG);
								if(configTree instanceof Map) {
									if(VALUE_TYPE_LOAD.equals(value)) {
										newSubContainer = new SingleJobContainer(
											(Map<String, Object>) configTree
										);
									} else {
										newSubContainer = new RampupJobContainer(
											(Map<String, Object>) configTree
										);
									}
									subContainer.append(newSubContainer);
									subContainer = newSubContainer;
								} else if(configTree == null) {
									if(VALUE_TYPE_LOAD.equals(value)) {
										newSubContainer = new SingleJobContainer();
									} else {
										newSubContainer = new RampupJobContainer();
									}
									subContainer.append(newSubContainer);
									subContainer = newSubContainer;
								} else {
									LOG.warn(
										Markers.ERR, "{}: config tree is \"{}\"",
										jobContainer, configTree.getClass()
									);
								}
								break;
							default:
								LOG.warn(
									Markers.ERR, "{}: unexpected value: {}", jobContainer, value
								);
						}
					} else {
						LOG.warn(
							Markers.ERR, "{}: unexpected value type: {}",
							jobContainer, value.getClass()
						);
					}
					break;
				case KEY_CONFIG:
					break; // ignore
				default:
					LOG.warn(Markers.ERR, "{}: unexpected key: {}", jobContainer, key);
			}
		}
	}
	//
	@Override
	public final void run() {
		super.run();
		LOG.info(Markers.MSG, "Scenario end");
	}
	//
	@Override
	public final String toString() {
		return "jsonScenario#" + hashCode();
	}
}
