package com.emc.mongoose.integ.tools;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by olga on 03.07.15.
 */
public final class LogParser {

	public static void flushAllLogs()
	throws IOException {
		for(final RunIdFileManager manager: RunIdFileManager.INSTANCES) {
			for (final OutputStream out: manager.getOutStreamsMap().values()){
				out.flush();
			}
		}
	}

	public static void removeLogDirectory(final String runID)
	throws Exception {
		final Path logDir = Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID);
		removeDirectory(logDir);
	}

	private static void removeDirectory(final Path path)
	throws Exception {
		final File dir = path.toFile();
		if (dir.listFiles() != null) {
			for (final File currFile : dir.listFiles()) {
				if (currFile.isDirectory()) {
					removeDirectory(currFile.getAbsoluteFile().toPath());
				}
				Files.deleteIfExists(currFile.toPath());
			}
		}
	}

	public static File getMessageFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.MESSAGE_FILE_NAME).toString());
	}

	public static File getPerfAvgFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.PERF_AVG_FILE_NAME).toString());
	}

	public static File getPerfSumFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.PERF_SUM_FILE_NAME).toString());
	}

	public static File getPerfTraceFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.PERF_TRACE_FILE_NAME).toString());
	}

	public static File getDataItemsFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.DATA_ITEMS_FILE_NAME).toString());
	}

	public static File getErrorsFile(final String runID){
		return new File(Paths.get(RunTimeConfig.DIR_ROOT,
			Constants.DIR_LOG, runID, TestConstants.ERR_FILE_NAME).toString());
	}
	//
	public static void assertCorrectPerfSumCSV(BufferedReader in)
	throws IOException {
		boolean firstRow = true;
		//
		final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
		for(final CSVRecord nextRec : recIter) {
			if (firstRow) {
				Assert.assertEquals("DateTimeISO8601", nextRec.get(0));
				Assert.assertEquals("LoadId", nextRec.get(1));
				Assert.assertEquals("TypeAPI", nextRec.get(2));
				Assert.assertEquals("TypeLoad", nextRec.get(3));
				Assert.assertEquals("CountConn", nextRec.get(4));
				Assert.assertEquals("CountNode", nextRec.get(5));
				Assert.assertEquals("CountLoadServer", nextRec.get(6));
				Assert.assertEquals("CountSucc", nextRec.get(7));
				Assert.assertEquals("CountFail", nextRec.get(8));
				Assert.assertEquals("LatencyAvg[us]", nextRec.get(9));
				Assert.assertEquals("LatencyMin[us]", nextRec.get(10));
				Assert.assertEquals("LatencyMed[us]", nextRec.get(11));
				Assert.assertEquals("LatencyMax[us]", nextRec.get(12));
				Assert.assertEquals("TPAvg", nextRec.get(13));
				Assert.assertEquals("TP1Min", nextRec.get(14));
				Assert.assertEquals("TP5Min", nextRec.get(15));
				Assert.assertEquals("TP15Min", nextRec.get(16));
				Assert.assertEquals("BWAvg[MB/s]", nextRec.get(17));
				Assert.assertEquals("BW1Min[MB/s]", nextRec.get(18));
				Assert.assertEquals("BW5Min[MB/s]", nextRec.get(19));
				Assert.assertEquals("BW15Min[MB/s]", nextRec.get(20));
				firstRow = false;
			} else if (nextRec.size() == 21) {
				Assert.assertTrue(
					"Data and time format is not correct",
					nextRec.get(0).matches(LogPatterns.DATE_TIME_ISO8601.pattern())
				);
				Assert.assertTrue(
					"Load ID is not correct", LogParser.isInteger(nextRec.get(1))
				);
				Assert.assertTrue(
					"API type format is not correct", nextRec.get(2).matches(LogPatterns.TYPE_API.pattern())
				);
				Assert.assertTrue(
					"Load type format is not correct",nextRec.get(3).matches(LogPatterns.TYPE_LOAD.pattern())
				);
				Assert.assertTrue(
					"Count of connection is not correct", LogParser.isInteger(nextRec.get(4))
				);
				Assert.assertTrue(
					"Count of node is not correct", LogParser.isInteger(nextRec.get(5))
				);
				Assert.assertTrue(
					"There are not load servers in run", nextRec.get(6).isEmpty()
				);
				Assert.assertTrue(
					"Count of success is not correct", LogParser.isInteger(nextRec.get(7))
				);
				Assert.assertTrue(
					"Count of fail is not correct", LogParser.isInteger(nextRec.get(8))
				);
				//
				Assert.assertTrue(
					"Latency avg is not correct", LogParser.isInteger(nextRec.get(9))
				);
				Assert.assertTrue(
					"Latency min is not correct", LogParser.isInteger(nextRec.get(10))
				);
				Assert.assertTrue(
					"Latency median is not correct", LogParser.isInteger(nextRec.get(11))
				);
				Assert.assertTrue(
					"Latency max is not correct", LogParser.isInteger(nextRec.get(12))
				);
				Assert.assertTrue(
					"TP avg is not correct", LogParser.isDouble(nextRec.get(13))
				);
				Assert.assertTrue(
					"TP 1 minutes is not correct", LogParser.isDouble(nextRec.get(14))
				);
				Assert.assertTrue(
					"TP  5 minutes is not correct", LogParser.isDouble(nextRec.get(15))
				);
				Assert.assertTrue(
					"TP  15 minutes is not correct", LogParser.isDouble(nextRec.get(16))
				);
				Assert.assertTrue(
					"BW avg is not correct", LogParser.isDouble(nextRec.get(17))
				);
				Assert.assertTrue(
					"BW 1 minutes is not correct", LogParser.isDouble(nextRec.get(18))
				);
				Assert.assertTrue(
					"BW  5 minutes is not correct", LogParser.isDouble(nextRec.get(19))
				);
				Assert.assertTrue(
					"BW  15 minutes is not correct", LogParser.isDouble(nextRec.get(20))
				);
			}
		}
	}
	//
	public static void assertCorrectPerfAvgCSV(BufferedReader in)
		throws IOException {
		boolean firstRow = true;

		final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
		for(final CSVRecord nextRec : recIter) {
			if(firstRow) {
				Assert.assertEquals("DateTimeISO8601", nextRec.get(0));
				Assert.assertEquals("LoadId", nextRec.get(1));
				Assert.assertEquals("TypeAPI", nextRec.get(2));
				Assert.assertEquals("TypeLoad", nextRec.get(3));
				Assert.assertEquals("CountConn", nextRec.get(4));
				Assert.assertEquals("CountNode", nextRec.get(5));
				Assert.assertEquals("CountLoadServer", nextRec.get(6));
				Assert.assertEquals("CountSucc", nextRec.get(7));
				Assert.assertEquals("CountPending", nextRec.get(8));
				Assert.assertEquals("CountFail", nextRec.get(9));
				Assert.assertEquals("LatencyAvg[us]", nextRec.get(10));
				Assert.assertEquals("LatencyMin[us]", nextRec.get(11));
				Assert.assertEquals("LatencyMed[us]", nextRec.get(12));
				Assert.assertEquals("LatencyMax[us]", nextRec.get(13));
				Assert.assertEquals("TPAvg", nextRec.get(14));
				Assert.assertEquals("TP1Min", nextRec.get(15));
				Assert.assertEquals("TP5Min", nextRec.get(16));
				Assert.assertEquals("TP15Min", nextRec.get(17));
				Assert.assertEquals("BWAvg[MB/s]", nextRec.get(18));
				Assert.assertEquals("BW1Min[MB/s]", nextRec.get(19));
				Assert.assertEquals("BW5Min[MB/s]", nextRec.get(20));
				Assert.assertEquals("BW15Min[MB/s]", nextRec.get(21));
				firstRow = false;
			} else {
				Assert.assertTrue(
					"Data and time format is not correct",
					nextRec.get(0).matches(LogPatterns.DATE_TIME_ISO8601.pattern())
				);
				Assert.assertTrue(
					"Load ID is not correct", LogParser.isInteger(nextRec.get(1))
				);
				Assert.assertTrue(
					"API type format is not correct", nextRec.get(2).matches(LogPatterns.TYPE_API.pattern())
				);
				Assert.assertTrue(
					"Load type format is not correct",nextRec.get(3).matches(LogPatterns.TYPE_LOAD.pattern())
				);
				Assert.assertTrue(
					"Count of connection is not correct", LogParser.isInteger(nextRec.get(4))
				);
				Assert.assertTrue(
					"Count of node is not correct", LogParser.isInteger(nextRec.get(5))
				);
				Assert.assertTrue(
					"There are not load servers in run", nextRec.get(6).isEmpty()
				);
				Assert.assertTrue(
					"Count of success is not correct", LogParser.isInteger(nextRec.get(7))
				);
				Assert.assertTrue(
					"Count of pending is not correct", LogParser.isInteger(nextRec.get(8))
				);
				Assert.assertTrue(
					"Count of fail is not correct", LogParser.isInteger(nextRec.get(9))
				);
				//
				Assert.assertTrue(
					"Latency avg is not correct", LogParser.isInteger(nextRec.get(10))
				);
				Assert.assertTrue(
					"Latency min is not correct", LogParser.isInteger(nextRec.get(11))
				);
				Assert.assertTrue(
					"Latency median is not correct", LogParser.isInteger(nextRec.get(12))
				);
				Assert.assertTrue(
					"Latency max is not correct", LogParser.isInteger(nextRec.get(13))
				);
				Assert.assertTrue(
					"TP avg is not correct", LogParser.isDouble(nextRec.get(14))
				);
				Assert.assertTrue(
					"TP 1 minutes is not correct", LogParser.isDouble(nextRec.get(15))
				);
				Assert.assertTrue(
					"TP  5 minutes is not correct", LogParser.isDouble(nextRec.get(16))
				);
				Assert.assertTrue(
					"TP  15 minutes is not correct", LogParser.isDouble(nextRec.get(17))
				);
				Assert.assertTrue(
					"BW avg is not correct", LogParser.isDouble(nextRec.get(18))
				);
				Assert.assertTrue(
					"BW 1 minutes is not correct", LogParser.isDouble(nextRec.get(19))
				);
				Assert.assertTrue(
					"BW  5 minutes is not correct", LogParser.isDouble(nextRec.get(20))
				);
				Assert.assertTrue(
					"BW  15 minutes is not correct", LogParser.isDouble(nextRec.get(21))
				);
			}
		}
	}
	//
	public static void assertCorrectDataItemsCSV(BufferedReader in)
		throws IOException {
		//
		final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
		for(final CSVRecord nextRec : recIter) {
			Assert.assertTrue(
				"Data ID format is not correct", nextRec.get(0).matches(LogPatterns.DATA_ID.pattern())
			);
			// Data offset has the same pattern as data ID
			Assert.assertTrue(
				"Data offset is not correct", nextRec.get(1).matches(LogPatterns.DATA_ID.pattern())
			);
			Assert.assertTrue(
				"Data size format is not correct", LogParser.isInteger(nextRec.get(2))
			);
			Assert.assertTrue(
				"Data layer and mask format is not correct",
				nextRec.get(3).matches(LogPatterns.DATA_LAYER_MASK.pattern())
			);
		}
	}
	//
	public static void assertCorrectPerfTraceCSV(BufferedReader in)
		throws IOException {
		boolean firstRow = true;
		//
		final Iterable<CSVRecord> recIter = CSVFormat.RFC4180.parse(in);
		for(final CSVRecord nextRec : recIter) {
			if (firstRow) {
				Assert.assertEquals("Thread", nextRec.get(0));
				Assert.assertEquals("TargetNode", nextRec.get(1));
				Assert.assertEquals("DataItemId", nextRec.get(2));
				Assert.assertEquals("DataItemSize", nextRec.get(3));
				Assert.assertEquals("StatusCode", nextRec.get(4));
				Assert.assertEquals("ReqTimeStart[us]", nextRec.get(5));
				Assert.assertEquals("Latency[us]", nextRec.get(6));
				Assert.assertEquals("Duration[us]", nextRec.get(7));
				firstRow = false;
			} else {
				Assert.assertTrue(
					"Thread name format is not correct", nextRec.get(0).matches(LogPatterns.THREAD_NAME.pattern())
				);
				Assert.assertTrue(
					"Target node is not correct", nextRec.get(1).matches(LogPatterns.TARGET_NODE.pattern())
				);
				Assert.assertTrue(
					"Data ID format is not correct", nextRec.get(2).matches(LogPatterns.DATA_ID.pattern())
				);
				Assert.assertTrue(
					"Data size format is not correct", LogParser.isInteger(nextRec.get(3))
				);
				Assert.assertTrue(
					"Status code and mask format is not correct", LogParser.isInteger(nextRec.get(4))
				);
				Assert.assertTrue(
					"Request time start format is not correct", LogParser.isLong(nextRec.get(5))
				);
				Assert.assertTrue(
					"Latency format is not correct", LogParser.isInteger(nextRec.get(6))
				);
				Assert.assertTrue(
					"Duration format is not correct", LogParser.isInteger(nextRec.get(7))
				);
			}
		}
	}
	//
	public static boolean isInteger(final String line){
		try {
			Integer.parseInt(line);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

	public static boolean isLong(final String line){
		try {
			Long.parseLong(line);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

	public static boolean isDouble(final String line){
		try {
			Double.parseDouble(line);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

}
