package com.emc.mongoose.run.scenario.engine;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
/**
 Created by kurila on 02.02.16.
 */
public class JsonScenario
extends SequentialJob
implements Scenario {
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
		super(config, tree);
	}
	//
	@Override
	protected final void loadSubTree(final Map<String, Object> subTree) {
		appendNewJob(subTree, localConfig);
	}
	//
	@Override
	protected final synchronized boolean append(final Job job) {
		if(childJobs.size() == 0) {
			return super.append(job);
		} else {
			return false;
		}
	}
	//
	@Override
	public final String toString() {
		return "jsonScenario#" + hashCode();
	}
}
