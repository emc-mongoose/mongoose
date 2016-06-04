package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.log.Markers;
//
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
	private final static String KEY_VALUE = "value";
	private final static String KEY_BLOCKING = "blocking";
	private final static String KEY_IN = "in";
	private final static String NODE_TYPE_PARALLEL = "parallel";
	private final static String NODE_TYPE_SEQUENTIAL = "sequential";
	private final static String NODE_TYPE_LOAD = "load";
	private final static String NODE_TYPE_PRECONDITION = "precondition";
	private final static String NODE_TYPE_RAMPUP = "rampup";
	private final static String NODE_TYPE_COMMAND = "command";
	private final static String NODE_TYPE_EACH = "each";
	//
	private final Map<String, Object> scenarioTree;
	//
	public JsonScenario(final AppConfig config, final File scenarioSrcFile)
	throws IOException, CloneNotSupportedException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioSrcFile, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final AppConfig config, final InputStream scenarioInputStream)
	throws IOException, CloneNotSupportedException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioInputStream, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final AppConfig config, final String scenarioString)
	throws IOException, CloneNotSupportedException {
		this(
			config,
			new ObjectMapper()
				.configure(JsonParser.Feature.ALLOW_COMMENTS, true)
				.configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true)
				.<Map<String, Object>>readValue(
					scenarioString, new TypeReference<Map<String, Object>>(){}
				)
		);
	}
	//
	public JsonScenario(final AppConfig config, final Map<String, Object> tree)
	throws IOException, CloneNotSupportedException {
		super(config);
		this.scenarioTree = tree;
		loadTree(scenarioTree, this);
	}
	//
	private static void loadTree(final Map<String, Object> node, final JobContainer jobContainer)
	throws IOException, CloneNotSupportedException {
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
						final Object configTree = node.get(KEY_CONFIG);
						final AppConfig nodeConfig = (AppConfig) subContainer.getConfig().clone();
						if(configTree instanceof Map) {
							if(nodeConfig != null) {
								nodeConfig.override(null, (Map<String, Object>) configTree);
							}
						}
						switch((String) value) {
							case NODE_TYPE_PARALLEL:
								newSubContainer = new ParallelJobContainer(nodeConfig);
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case NODE_TYPE_SEQUENTIAL:
								newSubContainer = new SequentialJobContainer(nodeConfig);
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case NODE_TYPE_EACH:
								newSubContainer = new EachJobContainer(
									nodeConfig, (String) node.get(KEY_VALUE),
									(List) node.get(KEY_IN)
								);
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case NODE_TYPE_COMMAND:
								final Object rawFlagValue = node.get(KEY_BLOCKING);
								newSubContainer = new CommandJobContainer(
									(String) node.get(KEY_VALUE),
									rawFlagValue == null ? true : (Boolean) rawFlagValue
								);
								subContainer.append(newSubContainer);
								subContainer = newSubContainer;
								break;
							case NODE_TYPE_LOAD:
							case NODE_TYPE_PRECONDITION:
							case NODE_TYPE_RAMPUP:
								if(configTree instanceof Map || configTree == null) {
									newSubContainer = createJobContainer(nodeConfig, value);
									subContainer.append(newSubContainer);
									subContainer = newSubContainer;
								} else {
									LOG.warn(Markers.ERR, "{}: config tree is \"{}\"", jobContainer,
										configTree.getClass()
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
				case KEY_VALUE:
				case KEY_BLOCKING:
				case KEY_IN:
					break; // ignore because the keys above are consumed already
				default:
					LOG.warn(Markers.ERR, "{}: unexpected key: {}", jobContainer, key);
			}
		}
	}
	//
	private static JobContainer createJobContainer(final AppConfig nodeConfig, final Object value) {
		final boolean preconditionFlag = NODE_TYPE_PRECONDITION
			.equals(value);
		if(NODE_TYPE_LOAD.equals(value) || preconditionFlag) {
			nodeConfig.setProperty(
				AppConfig.KEY_LOAD_PRECONDITION, preconditionFlag
			);
			return new SingleJobContainer(nodeConfig);
		} else {
			return new RampupJobContainer(nodeConfig);
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
	//
	@Override
	public final void close()
	throws IOException {
		scenarioTree.clear();
		super.close();
	}
}
