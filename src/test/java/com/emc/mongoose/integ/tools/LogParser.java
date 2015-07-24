package com.emc.mongoose.integ.tools;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.Assert;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.regex.Matcher;

/**
 * Created by olga on 03.07.15.
 */
public final class LogParser {

	//Headers
	public static final String HEADER_PERF_TRACE_FILE = "Thread,TargetNode,DataItemId,DataItemSize,StatusCode," +
		"ReqTimeStart[us],Latency[us],Duration[us]";
	public static final String HEADER_PERF_AVG_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode," +
		"CountLoadServer,CountSucc,CountPending,CountFail,LatencyAvg[us],LatencyMin[us],LatencyMed[us]," +
		"LatencyMax[us],TPAvg,TP1Min,TP5Min,TP15Min,BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s]";
	public static final String HEADER_PERF_SUM_FILE = "DateTimeISO8601,LoadId,TypeAPI,TypeLoad,CountConn,CountNode," +
		"CountLoadServer,CountSucc,CountFail,LatencyAvg[us],LatencyMin[us],LatencyMed[us]," +
		"LatencyMax[us],TPAvg,TP1Min,TP5Min,TP15Min,BWAvg[MB/s],BW1Min[MB/s],BW5Min[MB/s],BW15Min[MB/s]";

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
				firstRow = false;
				Assert.assertEquals(
					String.format("Not correct headers of perf.sum.csv file: %s", nextRec.toString()),
					nextRec.toString(), LogParser.HEADER_PERF_SUM_FILE
				);
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
				Assert.assertNull(
					"There are not load servers in run", nextRec.get(6)
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
				Assert.assertEquals(
					String.format("Not correct headers of perf.avg.csv file: %s", nextRec.toString()),
					nextRec.toString(), LogParser.HEADER_PERF_AVG_FILE
				);
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
				Assert.assertNull(
					"There are not load servers in run", nextRec.get(6)
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
				firstRow = false;
				Assert.assertEquals(
					String.format("Not correct headers of perf.trace.csv file: %s", nextRec.toString()),
					nextRec.toString(), LogParser.HEADER_PERF_TRACE_FILE
				);
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
					"Request time start format is not correct", LogParser.isInteger(nextRec.get(5))
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

	public static boolean isDouble(final String line){
		try {
			Double.parseDouble(line);
			return true;
		} catch (final NumberFormatException e) {
			return false;
		}
	}

}
