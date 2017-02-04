package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.tests.system.util.BufferingOutputStream;
import com.emc.mongoose.tests.system.util.LogPatterns;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.K;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;
import static com.emc.mongoose.common.env.DateUtil.FMT_DATE_ISO8601;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.apache.commons.io.FileUtils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import static com.emc.mongoose.model.io.task.IoTask.Status.CANCELLED;
import static com.emc.mongoose.model.io.task.IoTask.Status.SUCC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

/**
 Created by andrey on 19.01.17.
 */
public abstract class LoggingTestBase {

	protected static Logger LOG;
	protected static String JOB_NAME;
	protected static BufferingOutputStream STD_OUT_STREAM;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		final URL u = LoggingTestBase.class.getClassLoader().getResource("logging.json");
		System.setProperty("log4j.configurationFile", u.toString());
		LogUtil.init();
		JOB_NAME = ThreadContext.get(KEY_JOB_NAME);
		// remove previous logs if exist
		FileUtils.deleteDirectory(Paths.get(PathUtil.getBaseDir(), "log", JOB_NAME).toFile());
		LOG = LogManager.getLogger();
		STD_OUT_STREAM = new BufferingOutputStream(System.out);
	}
	
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		STD_OUT_STREAM.close();
		//LogUtil.shutdown();
	}

	private static List<String> getLogFileLines(final String fileName)
	throws IOException {
		final File logFile = Paths.get(PathUtil.getBaseDir(), "log", JOB_NAME, fileName).toFile();
		try(final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			return br.lines().collect(Collectors.toList());
		}
	}

	protected static List<String> getMessageLogLines()
	throws IOException {
		return getLogFileLines("messages.log");
	}

	protected static List<String> getErrorsLogLines()
	throws IOException {
		return getLogFileLines("errors.log");
	}

	protected static List<String> getConfigLogLines()
	throws IOException {
		return getLogFileLines("config.log");
	}

	protected static List<String> getPartsUploadLogLines()
	throws IOException {
		return getLogFileLines("parts.upload.log");
	}

	protected static List<CSVRecord> getLogFileCsvRecords(final String fileName)
	throws IOException {
		final File logFile = Paths.get(PathUtil.getBaseDir(), "log", JOB_NAME, fileName).toFile();
		try(final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			final CSVParser csvParser = CSVFormat.RFC4180.withHeader().parse(br);
			final List<CSVRecord> csvRecords = new ArrayList<>();
			for(final CSVRecord csvRecord : csvParser) {
				csvRecords.add(csvRecord);
			}
			return csvRecords;
		}
	}

	protected static List<CSVRecord> getMetricsMedLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.med.csv");
	}

	protected static List<CSVRecord> getMetricsMedTotalLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.med.total.csv");
	}

	protected static List<CSVRecord> getMetricsLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.csv");
	}

	protected static List<CSVRecord> getMetricsTotalLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.total.csv");
	}

	protected static List<CSVRecord> getIoTraceLogRecords()
	throws IOException {
		return getLogFileCsvRecords("io.trace.csv");
	}

	protected static void testMetricsLogRecords(
		final List<CSVRecord> metrics, final IoType expectedIoType, final int expectedConcurrency,
		final int expectedDriverCount, final SizeInBytes expectedItemDataSize,
		final long expectedMaxCount, final int expectedLoadJobTime, final long metricsPeriodSec
	) throws Exception {
		final int countRecords = metrics.size();
		if(expectedLoadJobTime > 0) {
			assertEquals(
				"Count of the metrics records: " + countRecords,
				expectedLoadJobTime, metricsPeriodSec * countRecords, metricsPeriodSec
			);
		}

		Date lastTimeStamp = null, nextDateTimeStamp;
		String ioTypeStr;
		int concurrencyLevel;
		int driverCount;
		long prevTotalBytes = Long.MIN_VALUE, totalBytes;
		long prevCountSucc = Long.MIN_VALUE, countSucc;
		long countFail;
		long avgItemSize;
		double prevJobDuration = Double.NaN, jobDuration;
		double prevDurationSum = Double.NaN, durationSum;
		double tpAvg, tpLast;
		double bwAvg, bwLast;
		double durAvg;
		int durMin, durLoQ, durMed, durHiQ, durMax;
		double latAvg;
		int latMin, latLoQ, latMed, latHiQ, latMax;

		for(final CSVRecord nextRecord : metrics) {
			nextDateTimeStamp = FMT_DATE_ISO8601.parse(nextRecord.get("DateTimeISO8601"));
			if(lastTimeStamp != null) {
				assertEquals(
					"Next metrics record is expected to be in " + metricsPeriodSec,
					metricsPeriodSec, (nextDateTimeStamp.getTime() - lastTimeStamp.getTime()) / K,
					((double) metricsPeriodSec) / 10
				);
			}
			lastTimeStamp = nextDateTimeStamp;
			ioTypeStr = nextRecord.get("TypeLoad").toUpperCase();
			assertEquals(ioTypeStr, expectedIoType.name(), ioTypeStr);
			concurrencyLevel = Integer.parseInt(nextRecord.get("Concurrency"));
			assertEquals(
				"Expected concurrency level: " + concurrencyLevel, expectedConcurrency,
				concurrencyLevel
			);
			driverCount = Integer.parseInt(nextRecord.get("DriverCount"));
			assertEquals("Expected driver count: " + driverCount, expectedDriverCount, driverCount);
			totalBytes = SizeInBytes.toFixedSize(nextRecord.get("Size"));
			if(prevTotalBytes == Long.MIN_VALUE) {
				assertTrue(Long.toString(totalBytes), totalBytes >= 0);
			} else {
				assertTrue(Long.toString(totalBytes), totalBytes >= prevTotalBytes);
			}
			prevTotalBytes = totalBytes;
			countSucc = Long.parseLong(nextRecord.get("CountSucc"));
			if(prevCountSucc == Long.MIN_VALUE) {
				assertTrue(Long.toString(countSucc), countSucc >= 0);
			} else {
				assertTrue(Long.toString(countSucc), countSucc >= prevCountSucc);
			}
			if(expectedMaxCount > 0) {
				assertTrue(Long.toString(countSucc), countSucc <= expectedMaxCount);
			}
			prevCountSucc = countSucc;
			countFail = Long.parseLong(nextRecord.get("CountFail"));
			assertTrue(Long.toString(countFail), countFail < 1);
			if(countSucc > 0) {
				avgItemSize = totalBytes / countSucc;
				assertEquals(
					"Actual average item size: " + avgItemSize, expectedItemDataSize.getAvg(),
					avgItemSize, expectedItemDataSize.getAvg() / 100
				);
			}
			jobDuration = Double.parseDouble(nextRecord.get("JobDuration[s]"));
			if(Double.isNaN(prevJobDuration)) {
				assertEquals(Double.toString(jobDuration), 0, jobDuration, 1);
			} else {
				assertEquals(
					Double.toString(jobDuration), prevJobDuration + metricsPeriodSec, jobDuration, 2
				);
			}
			prevJobDuration = jobDuration;
			durationSum = Double.parseDouble(nextRecord.get("DurationSum[s]"));
			if(Double.isNaN(prevDurationSum)) {
				assertTrue(durationSum >= 0);
			} else {
				assertTrue(durationSum >= prevDurationSum);
			}
			final double
				effEstimate = durationSum / (concurrencyLevel * driverCount * jobDuration);
			assertTrue(
				"Efficiency estimate: " + effEstimate + " = " + durationSum + " / (" + concurrencyLevel + " * " + driverCount + " * " + jobDuration + ")",
				effEstimate <= 1 && effEstimate >= 0
			);
			prevDurationSum = durationSum;
			tpAvg = Double.parseDouble(nextRecord.get("TPAvg[op/s]"));
			tpLast = Double.parseDouble(nextRecord.get("TPLast[op/s]"));
			bwAvg = Double.parseDouble(nextRecord.get("BWAvg[MB/s]"));
			bwLast = Double.parseDouble(nextRecord.get("BWLast[MB/s]"));
			assertEquals(bwAvg / tpAvg, bwAvg / tpAvg, expectedItemDataSize.getAvg() / 100);
			assertEquals(bwLast / tpLast, bwLast / tpLast, expectedItemDataSize.getAvg() / 100);
			durAvg = Double.parseDouble(nextRecord.get("DurationAvg[us]"));
			assertTrue(durAvg >= 0);
			durMin = Integer.parseInt(nextRecord.get("DurationMin[us]"));
			assertTrue(durAvg >= durMin);
			durLoQ = Integer.parseInt(nextRecord.get("DurationLoQ[us]"));
			assertTrue(durLoQ >= durMin);
			durMed = Integer.parseInt(nextRecord.get("DurationMed[us]"));
			assertTrue(durMed >= durLoQ);
			durHiQ = Integer.parseInt(nextRecord.get("DurationHiQ[us]"));
			assertTrue(durHiQ >= durMed);
			durMax = Integer.parseInt(nextRecord.get("DurationMax[us]"));
			assertTrue(durMax >= durHiQ);
			latAvg = Double.parseDouble(nextRecord.get("LatencyAvg[us]"));
			assertTrue(latAvg >= 0);
			latMin = Integer.parseInt(nextRecord.get("LatencyMin[us]"));
			assertTrue(latAvg >= latMin);
			latLoQ = Integer.parseInt(nextRecord.get("LatencyLoQ[us]"));
			assertTrue(latLoQ >= latMin);
			latMed = Integer.parseInt(nextRecord.get("LatencyMed[us]"));
			assertTrue(latMed >= latLoQ);
			latHiQ = Integer.parseInt(nextRecord.get("LatencyHiQ[us]"));
			assertTrue(latHiQ >= latMed);
			latMax = Integer.parseInt(nextRecord.get("LatencyMax[us]"));
			assertTrue(latMax >= latHiQ);
		}
	}

	protected static void testTotalMetricsLogRecords(
		final CSVRecord metrics,
		final IoType expectedIoType, final int expectedConcurrency, final int expectedDriverCount,
		final SizeInBytes expectedItemDataSize, final long expectedMaxCount,
		final int expectedLoadJobTime
	) throws Exception {
		try {
			FMT_DATE_ISO8601.parse(metrics.get("DateTimeISO8601"));
		} catch(final ParseException e) {
			fail(e.toString());
		}
		final String ioTypeStr = metrics.get("TypeLoad").toUpperCase();
		assertEquals(ioTypeStr, expectedIoType.name(), ioTypeStr);
		final int concurrencyLevel = Integer.parseInt(metrics.get("Concurrency"));
		assertEquals(Integer.toString(concurrencyLevel), expectedConcurrency, concurrencyLevel);
		final int driverCount = Integer.parseInt(metrics.get("DriverCount"));
		assertEquals(Integer.toString(driverCount), expectedDriverCount, driverCount);
		final long totalBytes = SizeInBytes.toFixedSize(metrics.get("Size"));
		assertTrue(Long.toString(totalBytes), totalBytes > 0);
		final long countSucc = Long.parseLong(metrics.get("CountSucc"));
		if(expectedMaxCount > 0) {
			assertEquals(expectedMaxCount, countSucc);
		}
		assertTrue(Long.toString(countSucc), countSucc > 0);
		final long countFail = Long.parseLong(metrics.get("CountFail"));
		assertTrue(Long.toString(countFail), countFail < 1);
		final long avgItemSize = totalBytes / countSucc;
		assertEquals(
			Long.toString(avgItemSize), expectedItemDataSize.getAvg(), avgItemSize,
			expectedItemDataSize.getAvg() / 100
		);
		final double jobDuration = Double.parseDouble(metrics.get("JobDuration[s]"));
		if(expectedLoadJobTime > 0) {
			assertEquals(Double.toString(jobDuration), expectedLoadJobTime, jobDuration, 2);
		}
		final double durationSum = Double.parseDouble(metrics.get("DurationSum[s]"));
		final double effEstimate = durationSum / (concurrencyLevel * driverCount * jobDuration);
		assertTrue(Double.toString(effEstimate), effEstimate <= 1 && effEstimate > 0);
		final double tpAvg = Double.parseDouble(metrics.get("TPAvg[op/s]"));
		final double tpLast = Double.parseDouble(metrics.get("TPLast[op/s]"));
		final double bwAvg = Double.parseDouble(metrics.get("BWAvg[MB/s]"));
		final double bwLast = Double.parseDouble(metrics.get("BWLast[MB/s]"));
		assertEquals(bwAvg / tpAvg, bwAvg / tpAvg, expectedItemDataSize.getAvg() / 100);
		assertEquals(bwLast / tpLast, bwLast / tpLast, expectedItemDataSize.getAvg() / 100);
		final double durAvg = Double.parseDouble(metrics.get("DurationAvg[us]"));
		assertTrue(durAvg >= 0);
		final int durMin = Integer.parseInt(metrics.get("DurationMin[us]"));
		assertTrue(durAvg >= durMin);
		final int durLoQ = Integer.parseInt(metrics.get("DurationLoQ[us]"));
		assertTrue(durLoQ >= durMin);
		final int durMed = Integer.parseInt(metrics.get("DurationMed[us]"));
		assertTrue(durMed >= durLoQ);
		final int durHiQ = Integer.parseInt(metrics.get("DurationHiQ[us]"));
		assertTrue(durHiQ >= durMed);
		final int durMax = Integer.parseInt(metrics.get("DurationMax[us]"));
		assertTrue(durMax >= durHiQ);
		final double latAvg = Double.parseDouble(metrics.get("LatencyAvg[us]"));
		assertTrue(latAvg >= 0);
		final int latMin = Integer.parseInt(metrics.get("LatencyMin[us]"));
		assertTrue(latAvg >= latMin);
		final int latLoQ = Integer.parseInt(metrics.get("LatencyLoQ[us]"));
		assertTrue(latLoQ >= latMin);
		final int latMed = Integer.parseInt(metrics.get("LatencyMed[us]"));
		assertTrue(latMed >= latLoQ);
		final int latHiQ = Integer.parseInt(metrics.get("LatencyHiQ[us]"));
		assertTrue(latHiQ >= latMed);
		final int latMax = Integer.parseInt(metrics.get("LatencyMax[us]"));
		assertTrue(latMax >= latHiQ);
	}

	protected static void testIoTraceRecord(
		final CSVRecord ioTraceRecord, final int ioTypeCodeExpected, final SizeInBytes sizeExpected
	) throws Exception {
		assertEquals(ioTypeCodeExpected, Integer.parseInt(ioTraceRecord.get("IoTypeCode")));
		final int actualStatusCode = Integer.parseInt(ioTraceRecord.get("StatusCode"));
		assertTrue(actualStatusCode == CANCELLED.ordinal() || actualStatusCode == SUCC.ordinal());
		final long duration = Long.parseLong(ioTraceRecord.get("Duration[us]"));
		final String latencyStr = ioTraceRecord.get("RespLatency[us]");
		if(latencyStr != null && !latencyStr.isEmpty()) {
			assertTrue(duration >= Long.parseLong(latencyStr));
		}
		final long size = Long.parseLong(ioTraceRecord.get("TransferSize"));
		assertTrue(
			"Expected the size " + sizeExpected.toString() + ", but got " + size,
			sizeExpected.getMin() <= size && size <= sizeExpected.getMax()
		);
	}
	
	protected static void testMetricsStdout(
		final String stdOutContent,
		final IoType expectedIoType, final int expectedConcurrency, final int expectedDriverCount,
		final SizeInBytes expectedItemDataSize, final long metricsPeriodSec
	) throws Exception {
		Date lastTimeStamp = null, nextDateTimeStamp;
		String ioTypeStr;
		int concurrencyLevel;
		int driverCount;
		long prevTotalBytes = Long.MIN_VALUE, totalBytes;
		long prevCountSucc = Long.MIN_VALUE, countSucc;
		long countFail;
		long avgItemSize;
		double prevJobDuration = Double.NaN, jobDuration;
		double prevDurationSum = Double.NaN, durationSum;
		double tpAvg, tpLast;
		double bwAvg, bwLast;
		double durAvg;
		int durMin, durMax;
		double latAvg;
		int latMin, latMax;
		
		final Matcher m = LogPatterns.STD_OUT_METRICS_SINGLE.matcher(stdOutContent);
		while(m.find()) {
			nextDateTimeStamp = FMT_DATE_ISO8601.parse(m.group("dateTime"));
			if(lastTimeStamp != null) {
				assertEquals(
					metricsPeriodSec, (nextDateTimeStamp.getTime() - lastTimeStamp.getTime()) / K,
					((double) metricsPeriodSec) / 10
				);
			}
			lastTimeStamp = nextDateTimeStamp;
			ioTypeStr = m.group("typeLoad").toUpperCase();
			assertEquals(ioTypeStr, expectedIoType.name(), ioTypeStr);
			concurrencyLevel = Integer.parseInt(m.group("concurrency"));
			assertEquals(Integer.toString(concurrencyLevel), expectedConcurrency, concurrencyLevel);
			driverCount = Integer.parseInt(m.group("driverCount"));
			assertEquals(Integer.toString(driverCount), expectedDriverCount, driverCount);
			totalBytes = SizeInBytes.toFixedSize(m.group("size"));
			if(prevTotalBytes == Long.MIN_VALUE) {
				assertTrue(Long.toString(totalBytes), totalBytes >= 0);
			} else {
				assertTrue(Long.toString(totalBytes), totalBytes >= prevTotalBytes);
			}
			prevTotalBytes = totalBytes;
			countSucc = Long.parseLong(m.group("countSucc"));
			if(prevCountSucc == Long.MIN_VALUE) {
				assertTrue(Long.toString(countSucc), countSucc >= 0);
			} else {
				assertTrue(Long.toString(countSucc), countSucc >= prevCountSucc);
			}
			prevCountSucc = countSucc;
			countFail = Long.parseLong(m.group("countFail"));
			assertTrue(Long.toString(countFail), countFail < 1);
			if(countSucc > 0) {
				avgItemSize = totalBytes / countSucc;
				assertEquals(
					Long.toString(avgItemSize), expectedItemDataSize.getAvg(), avgItemSize,
					expectedItemDataSize.getAvg() / 100
				);
			}
			jobDuration = Double.parseDouble(m.group("jobDur"));
			if(Double.isNaN(prevJobDuration)) {
				assertEquals(Double.toString(jobDuration), 0, jobDuration, 1);
			} else {
				assertEquals(
					Double.toString(jobDuration), prevJobDuration + metricsPeriodSec, jobDuration, 1
				);
			}
			prevJobDuration = jobDuration;
			durationSum = Double.parseDouble(m.group("sumDur"));
			if(Double.isNaN(prevDurationSum)) {
				assertTrue(durationSum >= 0);
			} else {
				assertTrue(durationSum >= prevDurationSum);
			}
			final double
				effEstimate = durationSum / (concurrencyLevel * driverCount * jobDuration);
			assertTrue(Double.toString(effEstimate), effEstimate <= 1 && effEstimate >= 0);
			prevDurationSum = durationSum;
			tpAvg = Double.parseDouble(m.group("tpMean"));
			tpLast = Double.parseDouble(m.group("tpLast"));
			bwAvg = Double.parseDouble(m.group("bwMean"));
			bwLast = Double.parseDouble(m.group("bwLast"));
			assertEquals(bwAvg / tpAvg, bwAvg / tpAvg, expectedItemDataSize.getAvg() / 100);
			assertEquals(bwLast / tpLast, bwLast / tpLast, expectedItemDataSize.getAvg() / 100);
			durAvg = Double.parseDouble(m.group("durAvg"));
			assertTrue(durAvg >= 0);
			durMin = Integer.parseInt(m.group("durMin"));
			assertTrue(durAvg >= durMin);
			durMax = Integer.parseInt(m.group("durMax"));
			assertTrue(durMax >= durAvg);
			latAvg = Double.parseDouble(m.group("latAvg"));
			assertTrue(latAvg >= 0);
			latMin = Integer.parseInt(m.group("latMin"));
			assertTrue(latAvg >= latMin);
			latMax = Integer.parseInt(m.group("latMax"));
			assertTrue(latMax >= latAvg);
		}
	}
}
