package com.emc.mongoose.integ.core.rampup;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.run.scenario.ScriptRunner;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Created by olga on 22.07.15.
 * HLUC: 1.5.3.1
 */
public class DefaultRampupTest {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;
	//
	private static String RUN_ID = DefaultRampupTest.class.getCanonicalName();
	private static final String	LIMIT_TIME = "30.seconds";
	private static final int COUNT_STEPS = 70;

	private static Logger LOG;

	private static RunTimeConfig rtConfig;

	@BeforeClass
	public static void before()
	throws Exception {
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefault());
		rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_TIME, LIMIT_TIME);
		rtConfig.set(RunTimeConfig.KEY_SCENARIO_NAME, TestConstants.SCENARIO_RAMPUP);
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		//  write
		executeLoadJob(rtConfig);
		STD_OUTPUT_STREAM.close();
	}

	private static void executeLoadJob(final RunTimeConfig rtConfig)
	throws Exception {
		LOG.info(Markers.MSG, rtConfig.toString());
		UniformDataSource.DEFAULT = new UniformDataSource();
		try (final BufferingOutputStream stdOutStream =
			     StdOutInterceptorTestSuite.getStdOutBufferingStream()) {
			//  Run mongoose default scenario in standalone mode
			new ScriptRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
	}

	@AfterClass
	public static void after()
		throws Exception {
		final Bucket bucket = new WSBucketImpl(
			(com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("s3").setProperties(rtConfig),
			TestConstants.BUCKET_NAME, false
		);
		bucket.delete(rtConfig.getStorageAddrs()[0]);
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(
			"Should report information about end of scenario run from console",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		Assert.assertTrue("messages.log file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(RUN_ID).toPath();
		Assert.assertTrue("perf.sum.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(RUN_ID).toPath();
		Assert.assertTrue("perf.trace.csv file must be contained", Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(RUN_ID).toPath();
		Assert.assertTrue("data.items.csv file must be contained", Files.exists(expectedFile));

		/*
		expectedFile = LogParser.getErrorsFile(RUN_ID).toPath();
		Assert.assertFalse("errors.log file must not be contained", Files.exists(expectedFile));
		*/
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
	throws Exception {
		final String[] runtimeConfCustomParam = RunTimeConfig.getContext().toString().split("\n");
		for (final String confParam : runtimeConfCustomParam) {
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(
					"Information about time limit must be correct in configuration table",
					confParam.contains(LIMIT_TIME)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(
					"Information about storage address must be correct in configuration table",
					confParam.contains("127.0.0.1")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(
					"Information about run mode must be correct in configuration table",
					confParam.contains(Constants.RUN_MODE_STANDALONE)
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				if (RUN_ID.length() >= 64) {
					Assert.assertTrue(confParam.contains(RUN_ID.substring(0, 63).trim()));
				} else {
					Assert.assertTrue(confParam.contains(RUN_ID));
				}
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(
					"Information about load limit by count must be correct in configuration table",
					confParam.contains("0")
				);
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(
					"Information about scenario name must be correct in configuration table",
					confParam.contains(TestConstants.SCENARIO_RAMPUP)
				);
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario End"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
		Assert.assertTrue("message.log file must be exist", messageFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(messageFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(line);
			Assert.assertTrue(
				"message.log file must contains information about scenario end",
				line.contains(TestConstants.SCENARIO_END_INDICATOR)
			);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		// Get perf.sum.csv file
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file
		final File dataItemFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue("data.items.csv file must be exist", dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		// Get perf.trace.csv file
		final File perfTraceFile = LogParser.getPerfTraceFile(RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist",perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfTraceCSV(in);
		}
	}

	@Test
	public void shouldContainedInformationAboutAllLoadsByStep()
	throws Exception {
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file must be exist", perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			boolean firstRow = true;
			int iterationCount = 4, stepsCount = 0;
			final Set<String> loadsSet = new HashSet<>();
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				if (firstRow) {
					firstRow = false;
				} else if (nextRec.size() == 21) {
					if (iterationCount == 4) {
						iterationCount = 0;
						stepsCount++;
						//
						Assert.assertTrue("There are not all load types in this step", loadsSet.isEmpty());
						loadsSet.clear();
						loadsSet.add(TestConstants.LOAD_CREATE);
						loadsSet.add(TestConstants.LOAD_READ);
						loadsSet.add(TestConstants.LOAD_UPDATE);
						loadsSet.add(TestConstants.LOAD_APPEND);
						loadsSet.add(TestConstants.LOAD_DELETE);
					} else {
						iterationCount++;
					}
					//
					Assert.assertTrue("This load already exist in this step", loadsSet.contains(nextRec.get(3)));
					loadsSet.remove(nextRec.get(3));
					Assert.assertNotEquals("Count of success equals 0 ", 0, nextRec.get(7));
				}
			}
			Assert.assertEquals("Steps counts must be equal" + COUNT_STEPS, COUNT_STEPS, stepsCount);
		}
	}
}