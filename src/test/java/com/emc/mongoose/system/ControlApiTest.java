package com.emc.mongoose.system;

import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.params.Concurrency;
import com.emc.mongoose.params.ItemSize;
import com.emc.mongoose.params.RunMode;
import com.emc.mongoose.params.StorageType;
import com.emc.mongoose.util.DirWithManyFilesDeleter;
import com.emc.mongoose.util.docker.MongooseEntryNodeContainer;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.io.json.JsonSchemaProviderBase;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 @author veronika K. on 01.11.18 */
public class ControlApiTest {

	private final String HOST = "http://localhost:" + MongooseEntryNodeContainer.BUNDLED_DEFAULTS.val("run-port");
	private final String SCENARIO_PATH = null; //default
	private final String containerItemOutputPath = MongooseEntryNodeContainer.getContainerItemOutputPath(
		getClass().getSimpleName()
	);
	private final String hostItemOutputPath = MongooseEntryNodeContainer.getHostItemOutputPath(
		getClass().getSimpleName()
	);
	private static final int TIMEOUT_IN_MILLIS = 5_000;
	private final MongooseEntryNodeContainer testContainer;
	private final String stepId = "CONTROLS_TEST";
	private final StorageType storageType = StorageType.ATMOS;
	private final RunMode runMode = RunMode.LOCAL;
	private final Concurrency concurrency = Concurrency.SINGLE;
	private final ItemSize itemSize = ItemSize.SMALL;

	public ControlApiTest()
	throws Exception {
		try {
			FileUtils.deleteDirectory(Paths.get(MongooseEntryNodeContainer.HOST_LOG_PATH.toString(), stepId).toFile());
		} catch(final IOException ignored) {
		}
		final List<String> env =
			System.getenv().entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).collect(Collectors.toList());
		final List<String> args = new ArrayList<>();
		// for FS
		args.add("--item-output-path=" + containerItemOutputPath);
		try {
			// for FS
			DirWithManyFilesDeleter.deleteExternal(hostItemOutputPath);
			DirWithManyFilesDeleter.deleteExternal(MongooseEntryNodeContainer.APP_HOME_DIR);
		} catch(final Exception e) {
			e.printStackTrace(System.err);
		}
		//use default scenario
		testContainer = new MongooseEntryNodeContainer(
			stepId, storageType, runMode, concurrency, itemSize.getValue(), SCENARIO_PATH, env, args
		);
	}

	private String loadDefaultConfig()
	throws Exception {
		final String config = Files
			.lines(loadDefaultConfigFile().toPath())
			.map(Object::toString)
			.collect(Collectors.joining("\n"));
		return config;
	}

	private File loadDefaultConfigFile()
	throws Exception {
		return new File(MongooseEntryNodeContainer.HOST_LOG_PATH + "/${ctx:step_id}/config.json");
	}

	@Before
	public void setUp() {
		testContainer.start();
		testContainer.await(TIMEOUT_IN_MILLIS, TimeUnit.MILLISECONDS);
	}

	@Test
	public void test()
	throws Exception {
		testConfig();
		testSchema();
	}

	private void testSchema()
	throws Exception {
		final String schemaStr = resultFromServer(HOST + "/config/schema");
		final SchemaProvider schemaProvider = new JsonSchemaProviderBase() {
			@Override
			public String id() {
				return null;
			}

			@Override
			protected final InputStream schemaInputStream()
			throws IOException {
				return new ByteArrayInputStream(schemaStr.getBytes());
			}
		};
		final Map schema = schemaProvider.schema();
		try {
			ConfigUtil.loadConfig(loadDefaultConfigFile(), schema);
		} catch(final Exception e) {
			e.printStackTrace();
			throw new AssertionError("Schema is wrong");
		}
	}

	private void testConfig()
	throws Exception {
		final String result = resultFromServer(HOST + "/config");
		Assert.assertEquals(result, loadDefaultConfig());
	}

	private String resultFromServer(final String urlPath)
	throws Exception {
		final String result;
		final URL url = new URL(urlPath);
		final URLConnection conn = url.openConnection();
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			result = br.lines().collect(Collectors.joining("\n"));
		}
		return result;
	}

	@After
	public void tearDown()
	throws IOException {
		testContainer.close();
	}
}
