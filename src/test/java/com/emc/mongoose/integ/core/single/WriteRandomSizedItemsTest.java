package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.suite.LoggingTestSuite;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.ContentGetter;
import com.emc.mongoose.integ.tools.TestConstants;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.run.scenario.ScriptRunner;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

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

/**
 * Created by olga on 08.07.15.
 * Covers TC #3 (name: "Write the data items w/ random size.", steps: all) in Mongoose Core Functional Testing
 * HLUC: 1.1.3.1
 */
public class WriteRandomSizedItemsTest {
	//
	private static BufferingOutputStream STD_OUTPUT_STREAM;

	private static final String RUN_ID = WriteRandomSizedItemsTest.class.getCanonicalName();
	private static final String
		DATA_SIZE_MIN = "10B",
		DATA_SIZE_MAX = "100KB";
	private static final long LIMIT_COUNT = 10;

	private static Logger LOG;

	@BeforeClass
	public static void before()
	throws Exception {
		//  remove log dir w/ previous logs
		LogParser.removeLogDirectory(RUN_ID);
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefault());
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, LIMIT_COUNT);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MAX, DATA_SIZE_MAX);
		rtConfig.set(RunTimeConfig.KEY_DATA_SIZE_MIN, DATA_SIZE_MIN);
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

	@Test
	public void shouldReportInformationAboutSummaryMetricsFromConsole()
	throws Exception {
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SUMMARY_INDICATOR));
		Assert.assertTrue(STD_OUTPUT_STREAM.toString()
			.contains(TestConstants.SCENARIO_END_INDICATOR));
	}

	@Test
	public void shouldReportScenarioEndToMessageLogFile()
	throws Exception {
		//Read message file and search "Scenario end"
		final File messageFile = LogParser.getMessageFile(RUN_ID);
		Assert.assertTrue(messageFile.exists());
		//
		try (final BufferedReader bufferedReader = new BufferedReader(new FileReader(messageFile))) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (line.contains(TestConstants.SCENARIO_END_INDICATOR)) {
					break;
				}
			}
			Assert.assertNotNull(line);
			//Check the message file contain report about scenario end. If not line = null.
			Assert.assertTrue(line.contains(TestConstants.SCENARIO_END_INDICATOR));
		}
	}

	@Test
	public void shouldCreateAllFilesWithLogs()
	throws Exception {
		Path expectedFile = LogParser.getMessageFile(RUN_ID).toPath();
		//Check that messages.log file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfAvgFile(RUN_ID).toPath();
		//Check that perf.avg.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfSumFile(RUN_ID).toPath();
		//Check that perf.sum.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getPerfTraceFile(RUN_ID).toPath();
		//Check that perf.trace.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getDataItemsFile(RUN_ID).toPath();
		//Check that data.items.csv file is contained
		Assert.assertTrue(Files.exists(expectedFile));

		expectedFile = LogParser.getErrorsFile(RUN_ID).toPath();
		//Check that errors.log file is not created
		Assert.assertFalse(Files.exists(expectedFile));
	}

	@Test
	public void shouldCreateCorrectDataItemsFile()
	throws Exception {
		// Get data.items.csv file of write scenario run
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
		// Get perf.sum.csv file of write scenario run
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
		// Get perf.avg.csv file of write scenario run
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
	public void shouldReportCorrectWrittenCountToSummaryLogFile()
	throws Exception {
		// Read perf.summary file of create scenario run
		final File perfSumFile = LogParser.getPerfSumFile(RUN_ID);

		// Check that file exists
		Assert.assertTrue(perfSumFile.exists());

		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(perfSumFile))) {
			//  read header of csv file
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
		// Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			int dataSize, countDataItems = 0, actualMinSize = Integer.MAX_VALUE, actualMaxSize = 0;
			String line;
			// Size of this set must be > 1. It's mean that there are different data items sizes
			final Set<Integer> dataItemSizes = new HashSet<>();

			//
			while ((line = bufferedReader.readLine()) != null) {
				// Get dataSize from each line
				dataSize = Integer.valueOf(line.split(",")[TestConstants.DATA_SIZE_COLUMN_INDEX]);
				dataItemSizes.add(dataSize);
				if (dataSize < actualMinSize) {
					actualMinSize = dataSize;
				} else if (dataSize > actualMaxSize) {
					actualMaxSize = dataSize;
				}
				countDataItems++;
			}
			// Check that there are 10 lines in data.items.csv file
			Assert.assertEquals(LIMIT_COUNT, countDataItems);
			// Check that there are different data sizes
			Assert.assertTrue(dataItemSizes.size() > 1);
			// Check data items min size is correct
			Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MIN) <= actualMinSize);
			// Check data items max size is correct
			Assert.assertTrue(SizeUtil.toSize(DATA_SIZE_MAX) >= actualMaxSize);
		}
	}

	@Test
	public void shouldGetDifferentObjectsFromServer()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			final Set<String> setOfChecksum = new HashSet<>();

			while ((line = bufferedReader.readLine()) != null) {
				dataID = line.split(",")[TestConstants.DATA_ID_COLUMN_INDEX];
				// Add each data checksum from set
				try (final InputStream inputStream = ContentGetter.getStream(dataID)) {
					setOfChecksum.add(DigestUtils.md2Hex(inputStream));
				}
			}
			// If size of set with checksums is less then
			// dataCount it's mean that some checksums are equals
			Assert.assertEquals(LIMIT_COUNT, setOfChecksum.size());
		}
	}

	@Test
	public void shouldGetAllObjectsFromServerAndDataSizeIsDefault()
	throws Exception {
		//Read data.items.csv file of create scenario run
		final File dataItemsFile = LogParser.getDataItemsFile(RUN_ID);
		Assert.assertTrue(dataItemsFile.exists());
		//
		try (final BufferedReader bufferedReader =
			     new BufferedReader(new FileReader(dataItemsFile))) {
			String line, dataID;
			int actualDataSize, expectedDataSize;
			String[] dataItemInfo;

			while ((line = bufferedReader.readLine()) != null) {
				dataItemInfo = line.split(",");
				dataID = dataItemInfo[TestConstants.DATA_ID_COLUMN_INDEX];
				expectedDataSize = Integer.valueOf(
						dataItemInfo[TestConstants.DATA_SIZE_COLUMN_INDEX]
				);
				actualDataSize = ContentGetter.getDataSize(dataID);
				Assert.assertEquals(expectedDataSize, actualDataSize);
			}
		}
	}
}
