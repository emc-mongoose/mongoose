package com.emc.mongoose.common.json;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.run.scenario.engine.Scenario;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.emc.mongoose.common.conf.AppConfig.FNAME_CONF;

/**
 Created on 31.05.16.
 */
public class JsonUtilTest {

	private static final String ROOT_DIR = "/home/ilya/IdeaProjects/freeGoose/mongoose";
	private static final Path PATH_TO_APP_CONFIG_DIR =
		Paths.get(ROOT_DIR, Constants.DIR_CONF).resolve(FNAME_CONF);
	private static final Path PATH_TO_SCENARIO_DIR =
		Paths.get(ROOT_DIR, Scenario.DIR_SCENARIO);

	@Test
	public void shouldCollectJsArrayPathContent()
	throws Exception {
		System.out.println(JsonUtil.jsArrayPathContent(PATH_TO_SCENARIO_DIR));
	}

	@Test
	public void shouldConvertJsonFileToString() throws Exception {
		System.out.println(JsonUtil.readFileToString(PATH_TO_APP_CONFIG_DIR, true));
	}
}