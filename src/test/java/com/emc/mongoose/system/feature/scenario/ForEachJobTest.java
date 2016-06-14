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

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import static com.emc.mongoose.system.tools.TestConstants.SCENARIO_END_INDICATOR;
import static com.emc.mongoose.system.tools.TestConstants.SUMMARY_INDICATOR;
/**
 Created by andrey on 08.06.16.
 */
public class ForEachJobTest
extends HttpStorageMockTestBase {

	private final static String RUN_ID = ForEachJobTest.class.getCanonicalName();
	private final static String SCENARIO_JSON =
		"{" +
		"	\"type\" : \"each\"," +
		"		\"value\" : \"size\"," +
		"		\"in\" : [" +
		"			0, \"1KB\", \"1MB\"" +
		"		]," +
		"		\"config\" : {" +
		"		\"item\" : {" +
		"			\"data\" : {" +
		"				\"size\" : \"${size}\"" +
		"			}" +
		"		}," +
		"		\"load\" : {" +
		"			\"limit\" : {" +
		"				\"count\" : 1000000," +
		"					\"time\" : \"10s\"" +
		"			}," +
		"			\"metricsPeriod\" : 0" +
		"		}" +
		"	}," +
		"		\"jobs\" : [" +
		"	{" +
		"		\"type\" : \"each\"," +
		"		\"value\" : \"threads\"," +
		"		\"in\" : [" +
		"			1, 10, 100" +
		"		]," +
		"		\"config\" : {" +
		"		\"item\" : {" +
		"			\"dst\" : {" +
		"				\"container\" : \"${size}_${threads}threads\"" +
		"			}" +
		"		}," +
		"		\"load\" : {" +
		"			\"threads\" : \"${threads}\"" +
		"		}" +
		"	}," +
		"		\"jobs\" : [" +
		"		{" +
		"			\"type\" : \"load\"," +
		"			\"config\" : {" +
		"			\"item\" : {" +
		"				\"dst\" : {" +
		"					\"file\" : \"" + RUN_ID + File.separator + "${size}_${threads}threads.csv\"" +
		"				}" +
		"			}," +
		"			\"load\" : {" +
		"				\"type\" : \"create\"" +
		"			}" +
		"		}" +
		"		}, {" +
		"		\"type\" : \"load\"," +
		"			\"config\" : {" +
		"			\"item\" : {" +
		"				\"src\" : {" +
		"					\"file\" : \"" + RUN_ID + File.separator + "${size}_${threads}threads.csv\"" +
		"				}" +
		"			}," +
		"			\"load\" : {" +
		"				\"type\" : \"read\"" +
		"			}" +
		"		}" +
		"	}, {" +
		"		\"type\" : \"load\"," +
		"			\"config\" : {" +
		"			\"item\" : {" +
		"				\"src\" : {" +
		"					\"file\" : \"" + RUN_ID + File.separator + "${size}_${threads}threads.csv\"" +
		"				}" +
		"			}," +
		"			\"load\" : {" +
		"				\"type\" : \"delete\"" +
		"			}" +
		"		}" +
		"	}" +
		"		]" +
		"	}" +
		"	]" +
		"}";
	private final static int EXPECTED_LOAD_JOB_COUNT = 27;
	private final static LoadType EXPECTED_LOAD_TYPE_SEQ[] = {
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE,
		LoadType.CREATE, LoadType.READ, LoadType.DELETE
	};
	private final static int EXPECTED_THREAD_COUNT_SEQ[] = {
		1, 1, 1, 10, 10, 10, 100, 100, 100,
		1, 1, 1, 10, 10, 10, 100, 100, 100,
		1, 1, 1, 10, 10, 10, 100, 100, 100,
	};

	private static String STD_OUTPUT;

	@BeforeClass
	public static void setUpClass() {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		try {
			HttpStorageMockTestBase.setUpClass();
			Files.createDirectory(Paths.get(RUN_ID));
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failure");
		}
		try(final BufferingOutputStream stdOutStream = StdOutUtil.getStdOutBufferingStream()) {
			try(
				final Scenario
					scenario = new JsonScenario(BasicConfig.THREAD_CONTEXT.get(), SCENARIO_JSON)
			) {
				scenario.run();
			} catch(final CloneNotSupportedException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the scenario");
			}
			TimeUnit.SECONDS.sleep(1);
			STD_OUTPUT = stdOutStream.toString();
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
	public void checkConsoleSummaryMetrics()
	throws Exception {
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
		Assert.assertEquals(EXPECTED_LOAD_JOB_COUNT, summaryCount);
		Assert.assertTrue(
			"Console output doesn't contain information about end of scenario",
			STD_OUTPUT.contains(SCENARIO_END_INDICATOR)
		);
	}

	@Test
	public void checkPerfSumFile()
	throws Exception {
		TimeUnit.SECONDS.sleep(5);
		//  Get perf.sum.csv file
		final File perfSumFile = LogValidator.getPerfSumFile(RUN_ID);
		Assert.assertTrue("perf.sum.csv file doesn't exist", perfSumFile.exists());
		//
		int summaryCount = 0;
		try(
			final BufferedReader in = Files.newBufferedReader(
				perfSumFile.toPath(), StandardCharsets.UTF_8
			)
		) {
			boolean firstRow = true;
			LoadType loadTypeExpected, loadTypeActual;
			int threadCountExpected, threadCountActual;
			final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
			for(final CSVRecord nextRec : recIter) {
				Assert.assertEquals(
					"Column count is wrong for the line: \"" + nextRec.toString() + "\"", 25,
					nextRec.size()
				);
				if(firstRow) {
					firstRow = false;
				} else {
					loadTypeExpected = EXPECTED_LOAD_TYPE_SEQ[summaryCount];
					loadTypeActual = LoadType.valueOf(nextRec.get(3).toUpperCase());
					Assert.assertEquals(nextRec.toString(), loadTypeExpected, loadTypeActual);
					threadCountExpected = EXPECTED_THREAD_COUNT_SEQ[summaryCount];
					threadCountActual = Integer.parseInt(nextRec.get(4));
					Assert.assertEquals(nextRec.toString(), threadCountExpected, threadCountActual);
					summaryCount ++;
				}
			}
		}
		Assert.assertEquals(EXPECTED_LOAD_JOB_COUNT, summaryCount);
	}

	@Test
	public void checkItemCsvFiles()
	throws Exception {
		final String sizeSeq[] = { "0", "1KB", "1MB" };
		final int threadCountSeq[] = { 1, 10, 100 };
		File nextItemCsvFile;
		for(String size : sizeSeq) {
			for(int threadCount : threadCountSeq) {
				nextItemCsvFile = new File(
					RUN_ID + File.separator + size + "_" + threadCount + "threads.csv"
				);
				Assert.assertTrue(
					"File \"" + nextItemCsvFile.toString() + "\" doesn't exist",
					nextItemCsvFile.exists()
				);
				try(
					final BufferedReader in = Files.newBufferedReader(
						nextItemCsvFile.toPath(), StandardCharsets.UTF_8
					)
				) {
					final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
					for(final CSVRecord nextRec : recIter) {
						Assert.assertEquals(
							"Unexpected item size for the record \"" + nextRec.toString() +
								"\" in the file \"" + nextItemCsvFile.toString() + "\"",
							SizeInBytes.toFixedSize(size), Integer.parseInt(nextRec.get(2))
						);
					}
				}
			}
		}
	}
}