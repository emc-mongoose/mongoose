package com.emc.mongoose.integ.core.single;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
//
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.ContentGetter;
import com.emc.mongoose.run.scenario.ScriptRunner;
//
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
//
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
//
/**
 * Created by olga on 30.06.15.
 * Covers TC #1 (name: "Write some data items.", steps: all) in Mongoose Core Functional Testing
 * HLUC: 1.1.1.1, 1.1.2.1, 1.3.1.1, 1.4.1.1, 1.5.3.1(1)
 */
public final class DefaultWriteTest {
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final int LIMIT_COUNT = 10;
	private static String RUN_ID = DefaultWriteTest.class.getCanonicalName();
	private static final String DATA_SIZE = "1MB";

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
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, TestConstants.BUCKET_NAME);
		LoggingTestSuite.setUpClass();

		LOG = LogManager.getLogger();
		LOG.info(Markers.MSG, rtConfig.toString());

		try (final BufferingOutputStream stdOutStream =
				StdOutInterceptorTestSuite.getStdOutBufferingStream()) {
			//  Run mongoose default scenario in standalone mode
			new ScriptRunner().run();
			//  Wait for "Scenario end" message
			TimeUnit.SECONDS.sleep(5);
			STD_OUTPUT_STREAM = stdOutStream;
		}
		STD_OUTPUT_STREAM.close();
	}

	@AfterClass
	public  static void after()
	throws Exception {

		final Bucket bucket = new WSBucketImpl(
			(com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("s3").setProperties(rtConfig),
			TestConstants.BUCKET_NAME, false
		);
		bucket.delete(rtConfig.getStorageAddrs()[0]);
	}

	@Test
	public void shouldReportInformationAboutSummaryMetricsToConsole()
	throws Exception {
		Assert.assertTrue("Console doesn't contain information about summary metrics",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SUMMARY_INDICATOR));
		Assert.assertTrue("Console doesn't contain information about end of scenario",
			STD_OUTPUT_STREAM.toString().contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		//  Check that messages.log exists
		Assert.assertTrue("messages.log file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(RUN_ID).toPath();
		//  Check that perf.avg.csv file exists
		Assert.assertTrue("perf.avg.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(RUN_ID).toPath();
		//  Check that perf.sum.csv file exists
		Assert.assertTrue("perf.sum.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(RUN_ID).toPath();
		//  Check that perf.trace.csv file exists
		Assert.assertTrue("perf.trace.csv file doesn't exist", Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(RUN_ID).toPath();
		//  Check that data.items.csv file exists
		Assert.assertTrue("data.items.csv file doesn't exist", Files.exists(expectedFile));
	}

	@Test
	public void shouldCustomValuesDisplayedCorrectlyInConfigurationTable()
		throws Exception {
		final String configTable = RunTimeConfig.getContext().toString();
		final Set<String> params = new HashSet<>();
		//  skip table header
		int start = 126;
		int lineOffset = 100;
		while (start + lineOffset < configTable.length()) {
			params.add(configTable.substring(start, start + lineOffset));
			start += lineOffset;
		}
		for (final String confParam : params) {
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_COUNT)) {
				Assert.assertTrue(confParam.contains(String.valueOf(LIMIT_COUNT)));
			}
			if (confParam.contains(RunTimeConfig.KEY_STORAGE_ADDRS)) {
				Assert.assertTrue(confParam.contains("127.0.0.1"));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_MODE)) {
				Assert.assertTrue(confParam.contains(Constants.RUN_MODE_STANDALONE));
			}
			if (confParam.contains(RunTimeConfig.KEY_RUN_ID)) {
				if (RUN_ID.length() >= 64) {
					Assert.assertTrue(confParam.contains(RUN_ID.substring(0, 63).trim()));
				} else {
					Assert.assertTrue(confParam.contains(RUN_ID));
				}
			}
			if (confParam.contains(RunTimeConfig.KEY_LOAD_LIMIT_TIME)) {
				Assert.assertTrue(confParam.contains("0"));
			}
			if (confParam.contains(RunTimeConfig.KEY_SCENARIO_NAME)) {
				Assert.assertTrue(confParam.contains(TestConstants.SCENARIO_SINGLE));
			}
		}
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//  Read the message file and search for "Scenario end"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
		Assert.assertTrue(messageFile.exists());
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
			Assert.assertTrue(line.contains(TestConstants.SCENARIO_END_INDICATOR));
		}
	}

	@Test
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		//  Read perf.summary file
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);

		//  Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		try (final BufferedReader bufferedReader =
			    new BufferedReader(new FileReader(perfSumFile))) {
			//  read header from csv file
			bufferedReader.readLine();

			// Get value of "CountSucc" column
			final int actualCountSucc = Integer.valueOf(
				bufferedReader.readLine().split(",")[TestConstants.COUNT_SUCC_COLUMN_INDEX]
			);
			Assert.assertEquals(LIMIT_COUNT, actualCountSucc);
		}
	}

	@Test
	public void shouldCreateDataItemsFileWithInformationAboutAllObjects()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
		        new BufferedReader(new FileReader(dataItemsFile))) {
			int dataSize, countDataItems = 0;
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				//  Get dataSize from each line
				dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), dataSize);
				countDataItems++;
			}
			//  Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(LIMIT_COUNT, countDataItems);
		}
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
		        new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			final Set<String> setOfChecksum = new HashSet<>();

			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				//  Add each data checksum from set
				try (final InputStream inputStream = ContentGetter.getStream(dataID, TestConstants.BUCKET_NAME)) {
					setOfChecksum.add(DigestUtils.md2Hex(inputStream));
				}
			}
			//  If size of set with checksums is less then dataCount
			//  it's mean that some checksums are equals
			Assert.assertEquals("Did not read different objects from server mock",
				LIMIT_COUNT, setOfChecksum.size());
		}
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception {
		//  Read data.items.csv file
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			int actualDataSize;

			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				actualDataSize = ContentGetter.getDataSize(dataID, TestConstants.BUCKET_NAME);
				Assert.assertEquals(SizeUtil.toSize(DATA_SIZE), actualDataSize);
			}
		}
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		//  Get data.items.csv file
		final File dataItemFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(dataItemFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectDataItemsCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfSumFile()
	throws Exception {
		//  Get perf.sum.csv file
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);
		Assert.assertTrue(perfSumFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfSumFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfSumCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfAvgFile()
	throws Exception {
		//  Get perf.avg.csv file
		final File perfAvgFile = LogParser.getPerfAvgFile(RUN_ID);
		Assert.assertTrue("perfAvg.csv file doesn't exist", perfAvgFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfAvgFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfAvgCSV(in);
		}
	}

	@Test
	public void shouldCreateCorrectPerfTraceFile()
	throws Exception {
		//  Get perf.trace.csv file
		final File perfTraceFile = LogParser.getPerfTraceFile(RUN_ID);
		Assert.assertTrue("perf.trace.csv file doesn't exist", perfTraceFile.exists());
		//
		try(
			final BufferedReader
				in = Files.newBufferedReader(perfTraceFile.toPath(), StandardCharsets.UTF_8)
		) {
			LogParser.assertCorrectPerfTraceCSV(in);
		}
	}
}
