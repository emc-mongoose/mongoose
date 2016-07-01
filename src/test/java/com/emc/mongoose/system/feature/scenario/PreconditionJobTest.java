package com.emc.mongoose.system.feature.scenario;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.run.scenario.engine.JsonScenario;
import com.emc.mongoose.run.scenario.engine.Scenario;
import com.emc.mongoose.system.base.HttpStorageMockTestBase;
import com.emc.mongoose.system.tools.BufferingOutputStream;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.system.tools.StdOutUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.emc.mongoose.run.scenario.engine.Scenario.*;
import static com.emc.mongoose.system.tools.TestConstants.SCENARIO_END_INDICATOR;
import static com.emc.mongoose.system.tools.TestConstants.SUMMARY_INDICATOR;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 08.06.16.
 */
public class PreconditionJobTest
extends HttpStorageMockTestBase {
	private final static String RUN_ID = PreconditionJobTest.class.getCanonicalName();
	private final static SizeInBytes ITEM_SIZE = new SizeInBytes("4KB");
	private final static Map<String, Object> SCENARIO_TREE = new HashMap<String, Object>() {{
		put(KEY_NODE_TYPE, NODE_TYPE_SEQUENTIAL);
		put(
			KEY_NODE_CONFIG,
			new HashMap<String, Object>() {{
				put(
					"item",
					new HashMap<String, Object>() {{
						put(
							"data",
							new HashMap<String, Object>() {{
								put("size", ITEM_SIZE.toString());
							}}
						);
						put(
							"dst",
							new HashMap<String, Object>() {{
								put("container", RUN_ID);
							}}
						);
					}}
				);
				put(
					"load",
					new HashMap<String, Object>() {{
						put(
							"limit",
							new HashMap<String, Object>() {{
								put("count", 1000);
							}}
						);
					}}
				);
			}}
		);
		put(
			KEY_NODE_JOBS,
			new ArrayList<Map<String, Object>>() {{
				add(
					new HashMap<String, Object>() {{
						put(KEY_NODE_TYPE, NODE_TYPE_PRECONDITION);
						put(
							KEY_NODE_CONFIG,
							new HashMap<String, Object>() {{
								put(
									"item",
									new HashMap<String, Object>() {{
										put(
											"dst",
											new HashMap<String, Object>() {{
												put("file", RUN_ID + File.separator + "items.csv");
											}}
										);
									}}
								);
							}}
						);
					}}
				);
				add(
					new HashMap<String, Object>() {{
						put(KEY_NODE_TYPE, NODE_TYPE_LOAD);
						put(
							KEY_NODE_CONFIG,
							new HashMap<String, Object>() {{
								put(
									"item",
									new HashMap<String, Object>() {{
										put(
											"src",
											new HashMap<String, Object>() {{
												put("file", RUN_ID + File.separator + "items.csv");
											}}
										);
									}}
								);
								put(
									"load",
									new HashMap<String, Object>() {{
										put("type", LoadType.READ.name().toLowerCase());
									}}
								);
							}}
						);
					}}
				);
			}}
		);
	}};

	private static String STD_OUTPUT;

	@BeforeClass
	public static void setUpClass() {
		final Path tgtDirPath = Paths.get(RUN_ID);
		if(!Files.exists(tgtDirPath)) {
			try {
				Files.createDirectory(Paths.get(RUN_ID));
			} catch(final IOException e) {
				e.printStackTrace(System.out);
				Assert.fail();
			}
		}
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		try {
			HttpStorageMockTestBase.setUpClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(
			final Scenario scenario = new JsonScenario(
				BasicConfig.THREAD_CONTEXT.get(), SCENARIO_TREE
			)
		) {
			try(final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()) {
				scenario.run();
				TimeUnit.SECONDS.sleep(1);
				STD_OUTPUT = stdOutStream.toString();
			}
		} catch(final Exception e) {
			e.printStackTrace(System.out);
		}
	}

	@AfterClass
	public static void tearDownClass() {
		try {
			HttpStorageMockTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		final File d = new File(RUN_ID);
		for(final File f : d.listFiles()) {
			f.delete();
		}
		d.delete();
	}

	@Test
	public void checkConsoleOutput() {
		int i = 0;
		int summaryCount = 0;
		while(i < STD_OUTPUT.length()) {
			i = STD_OUTPUT.indexOf(SUMMARY_INDICATOR, i);
			if(i > 0) {
				i ++;
				summaryCount ++;
			} else {
				break;
			}
		}
		Assert.assertEquals(2, summaryCount);
		Assert.assertTrue(
			"Console output doesn't contain information about end of scenario",
			STD_OUTPUT.contains(SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void checkPerfAvgFile()
	throws Exception {
		final String expectedLoadType = LoadType.READ.name().toLowerCase();
		final File perfAvgFile = LogValidator.getPerfAvgFile(RUN_ID);
		try(final BufferedReader in = Files.newBufferedReader(perfAvgFile.toPath(), UTF_8)) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			boolean firstRow = true;
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					Assert.assertEquals(expectedLoadType, nextRec.get(3).toLowerCase());
				}
			}
		}
	}

	@Test
	public void checkPerfSumFile()
	throws Exception {
		final String expectedLoadType = LoadType.READ.name().toLowerCase();
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		try(final BufferedReader in = Files.newBufferedReader(perfSumFile.toPath(), UTF_8)) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			boolean firstRow = true;
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					Assert.assertEquals(expectedLoadType, nextRec.get(3).toLowerCase());
				}
			}
		}
	}

	@Test
	public void checkPerfTraceFile()
	throws Exception {
		final String expectedLoadType = LoadType.READ.name();
		final File perfTraceFile = LogValidator.getPerfTraceFile(RUN_ID);
		try(final BufferedReader in = Files.newBufferedReader(perfTraceFile.toPath(), UTF_8)) {
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			boolean firstRow = true;
			for(final CSVRecord nextRec : recIter) {
				if(firstRow) {
					firstRow = false;
				} else {
					Assert.assertEquals(nextRec.get(1), expectedLoadType);
				}
			}
		}
	}
}
