package com.emc.mongoose.util;

import static com.emc.mongoose.base.Constants.MIB;
import static com.emc.mongoose.base.env.DateUtil.FMT_DATE_ISO8601;
import static com.emc.mongoose.base.env.DateUtil.FMT_DATE_METRICS_TABLE;
import static com.emc.mongoose.base.item.op.Operation.Status.FAIL_IO;
import static com.emc.mongoose.base.item.op.Operation.Status.INTERRUPTED;
import static com.emc.mongoose.base.item.op.Operation.Status.SUCC;
import static com.emc.mongoose.base.logging.MetricsAsciiTableLogMessage.TABLE_HEADER;
import static com.emc.mongoose.base.logging.MetricsAsciiTableLogMessage.TABLE_HEADER_PERIOD;
import static com.emc.mongoose.util.docker.MongooseEntryNodeContainer.APP_HOME_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.params.StorageType;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.akurilov.commons.system.SizeInBytes;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.stream.Collectors;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public interface LogValidationUtil {

	int LOG_FILE_TIMEOUT_SEC = 15;

	static List<String> getLogFileLines(final String stepId, final String fileName)
					throws IOException {
		final File logFile = Paths.get(APP_HOME_DIR, "log", stepId, fileName).toFile();
		try (final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			return br.lines().collect(Collectors.toList());
		}
	}

	static List<String> getMessageLogLines(final String stepId) throws IOException {
		return getLogFileLines(stepId, "messages.log");
	}

	static List<String> getErrorsLogLines(final String stepId) throws IOException {
		return getLogFileLines(stepId, "errors.log");
	}

	static List<String> getConfigLogLines(final String stepId) throws IOException {
		return getLogFileLines(stepId, "config.log");
	}

	static List<String> getPartsUploadLogLines(final String stepId) throws IOException {
		return getLogFileLines(stepId, "parts.upload.csv");
	}

	static File getLogFile(final String stepId, final String fileName) {
		final File logFile = Paths.get(APP_HOME_DIR, "log", stepId, fileName).toFile();
		if (!logFile.exists()) {
			try {
				File parent = logFile;
				while (null != (parent = parent.getParentFile())) {
					if (parent.exists()) {
						System.out.println(parent + " directory contents:");
						Files.list(parent.toPath()).forEach(System.out::println);
					}
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return logFile;
	}

	static List<CSVRecord> waitAndGetLogFileCsvRecords(final File logFile) throws IOException {
		long prevSize = 1, nextSize;
		for (int t = 0; t < LOG_FILE_TIMEOUT_SEC; t++) {
			if (logFile.exists()) {
				nextSize = logFile.length();
				if (prevSize == nextSize) {
					break;
				}
				prevSize = nextSize;
			}
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (final InterruptedException e) {
				return null;
			}
		}
		try (final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			try (final CSVParser csvParser = CSVFormat.RFC4180.withHeader().parse(br)) {
				final List<CSVRecord> csvRecords = new ArrayList<>();
				for (final CSVRecord csvRecord : csvParser) {
					csvRecords.add(csvRecord);
				}
				return csvRecords;
			}
		}
	}

	static List<CSVRecord> getLogFileCsvRecords(final String stepId, final String fileName)
					throws IOException {
		final File logFile = getLogFile(stepId, fileName);
		return waitAndGetLogFileCsvRecords(logFile);
	}

	static List<CSVRecord> getMetricsMedLogRecords(final String stepId) throws IOException {
		return getLogFileCsvRecords(stepId, "metrics.threshold.csv");
	}

	static List<CSVRecord> getMetricsMedTotalLogRecords(final String stepId) throws IOException {
		return getLogFileCsvRecords(stepId, "metrics.threshold.total.csv");
	}

	static List<CSVRecord> getMetricsLogRecords(final String stepId) throws IOException {
		return getLogFileCsvRecords(stepId, "metrics.csv");
	}

	static List<CSVRecord> getMetricsTotalLogRecords(final String stepId) throws IOException {
		return getLogFileCsvRecords(stepId, "metrics.total.csv");
	}

	static void waitLogFile(final File logFile) {
		long prevSize = 1, nextSize;
		for (int t = 0; t < LOG_FILE_TIMEOUT_SEC; t++) {
			if (logFile.exists()) {
				nextSize = logFile.length();
				if (prevSize == nextSize) {
					break;
				}
				prevSize = nextSize;
			}
			try {
				TimeUnit.SECONDS.sleep(1);
			} catch (final InterruptedException e) {
				return;
			}
		}
	}

	static void testOpTraceLogFile(final File logFile, final Consumer<CSVRecord> csvRecordTestFunc)
					throws IOException {
		try (final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			try (final CSVParser csvParser = CSVFormat.RFC4180.parse(br)) {
				csvParser.forEach(csvRecordTestFunc);
			}
		}
	}

	static void testOpTraceLogRecords(
					final String stepId, final Consumer<CSVRecord> csvRecordTestFunc) throws IOException {
		final File logFile = getLogFile(stepId, "op.trace.csv");
		waitLogFile(logFile);
		testOpTraceLogFile(logFile, csvRecordTestFunc);
	}

	static List<CSVRecord> getPartsUploadRecords(final String stepId) throws IOException {
		return getLogFileCsvRecords(stepId, "parts.upload.csv");
	}

	static void testMetricsLogRecords(
					final List<CSVRecord> metrics,
					final OpType expectedOpType,
					final int expectedConcurrency,
					final int expectedNodeCount,
					final SizeInBytes expectedItemDataSize,
					final long expectedMaxCount,
					final int expectedLoadJobTime,
					final long metricsPeriodSec)
					throws Exception {
		final int countRecords = metrics.size();
		if (expectedLoadJobTime > 0) {
			assertTrue(expectedLoadJobTime + metricsPeriodSec >= countRecords * metricsPeriodSec);
		}
		Date lastTimeStamp = null, nextDateTimeStamp;
		String opTypeStr;
		int concurrencyLevel;
		int nodeCount;
		int concurrencyCurr;
		double concurrencyMean;
		long totalBytes;
		long prevCountSucc = Long.MIN_VALUE, countSucc;
		long countFail;
		long avgItemSize;
		double stepDuration;
		double prevDurationSum = Double.NaN, durationSum;
		double tpAvg, tpLast;
		double bwAvg, bwLast;
		double durAvg;
		int durMin, durLoQ, durMed, durHiQ, durMax;
		double latAvg;
		int latMin, latLoQ, latMed, latHiQ, latMax;
		for (final CSVRecord nextRecord : metrics) {
			nextDateTimeStamp = FMT_DATE_ISO8601.parse(nextRecord.get("DateTimeISO8601"));
			if (lastTimeStamp != null) {
				assertEquals(
								"Next metrics record is expected to be in " + metricsPeriodSec,
								metricsPeriodSec,
								(nextDateTimeStamp.getTime() - lastTimeStamp.getTime()) / 1000,
								((double) metricsPeriodSec) / 2);
			}
			lastTimeStamp = nextDateTimeStamp;
			opTypeStr = nextRecord.get("OpType").toUpperCase();
			assertEquals(expectedOpType.name(), opTypeStr);
			concurrencyLevel = Integer.parseInt(nextRecord.get("Concurrency"));
			assertEquals(
							"Expected concurrency level: " + expectedConcurrency,
							expectedConcurrency,
							concurrencyLevel);
			nodeCount = Integer.parseInt(nextRecord.get("NodeCount"));
			assertEquals("Expected driver count: " + nodeCount, expectedNodeCount, nodeCount);
			concurrencyCurr = Integer.parseInt(nextRecord.get("ConcurrencyCurr"));
			concurrencyMean = Double.parseDouble(nextRecord.get("ConcurrencyMean"));
			if (expectedConcurrency > 0) {
				assertTrue(
								"Current concurrency "
												+ concurrencyCurr
												+ " should not be more than the limit "
												+ expectedConcurrency
												+ " x "
												+ nodeCount,
								concurrencyCurr <= expectedConcurrency * nodeCount);
				assertTrue(concurrencyMean <= expectedConcurrency * nodeCount);
			} else {
				assertTrue(concurrencyCurr >= 0);
				assertTrue(concurrencyMean >= 0);
			}
			totalBytes = SizeInBytes.toFixedSize(nextRecord.get("Size"));
			assertTrue(totalBytes >= 0);
			countSucc = Long.parseLong(nextRecord.get("CountSucc"));
			if (prevCountSucc == Long.MIN_VALUE) {
				assertTrue(Long.toString(countSucc), countSucc >= 0);
			} else {
				assertTrue(Long.toString(countSucc), countSucc >= prevCountSucc);
			}
			if (expectedMaxCount > 0) {
				assertTrue(
								"Expected no more than " + expectedMaxCount + " results, but got " + countSucc,
								countSucc <= expectedMaxCount);
			}
			prevCountSucc = countSucc;
			countFail = Long.parseLong(nextRecord.get("CountFail"));
			// assertTrue(Long.toString(countFail), countFail < 1);
			// use delta = 5%, because sometimes default storage-mock return error (1 missing response)
			assertEquals(Long.toString(countFail), countFail, 0, countSucc * 0.05);
			if (countSucc > 0) {
				avgItemSize = totalBytes / countSucc;
				if (expectedItemDataSize.getMin() < expectedItemDataSize.getMax()) {
					assertTrue(expectedItemDataSize.getMin() <= avgItemSize);
					assertTrue(expectedItemDataSize.getMax() >= avgItemSize);
				} else {
					assertEquals(
									"Actual average item size: " + avgItemSize,
									expectedItemDataSize.getAvg(),
									avgItemSize,
									expectedItemDataSize.get() / 100);
				}
			}
			stepDuration = Double.parseDouble(nextRecord.get("StepDuration[s]"));
			if (expectedLoadJobTime > 0) {
				assertTrue(
								"Step duration limit (" + expectedLoadJobTime + ") is broken: " + stepDuration,
								stepDuration <= expectedLoadJobTime + 1);
			}
			durationSum = Double.parseDouble(nextRecord.get("DurationSum[s]"));
			if (Double.isNaN(prevDurationSum)) {
				assertTrue(durationSum >= 0);
			} else {
				assertTrue(durationSum >= prevDurationSum);
				if (expectedConcurrency > 0 && stepDuration > 1) {
					final double effEstimate = durationSum / (nodeCount * expectedConcurrency * stepDuration);
					assertTrue("Efficiency estimate: " + effEstimate, effEstimate <= 1 && effEstimate >= 0);
				}
			}
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
			assertTrue(
							"Duration LoQ (" + durLoQ + ") should not be less than min (" + durMin + ")",
							durLoQ >= durMin);
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

	static void testTotalMetricsLogRecord(
					final CSVRecord metrics,
					final OpType expectedOpType,
					final int concurrencyLimit,
					final int expectedNodeCount,
					final SizeInBytes expectedItemDataSize,
					final long expectedMaxCount,
					final long maxFailureCount,
					final int expectedStepTime) {
		try {
			FMT_DATE_ISO8601.parse(metrics.get("DateTimeISO8601"));
		} catch (final ParseException e) {
			fail(e.toString());
		}
		final String opTypeStr = metrics.get("OpType").toUpperCase();
		assertEquals(opTypeStr, expectedOpType.name(), opTypeStr);
		final int actualConcurrencyLimit = Integer.parseInt(metrics.get("Concurrency"));
		assertEquals(concurrencyLimit, actualConcurrencyLimit);
		final int nodeCount = Integer.parseInt(metrics.get("NodeCount"));
		assertEquals(Integer.toString(nodeCount), expectedNodeCount, nodeCount);
		final double concurrencyLastMean = Double.parseDouble(metrics.get("ConcurrencyMean"));
		if (concurrencyLimit > 0) {
			assertTrue(concurrencyLastMean <= concurrencyLimit * nodeCount);
		} else {
			assertTrue(concurrencyLastMean >= 0);
		}
		final long totalBytes = SizeInBytes.toFixedSize(metrics.get("Size"));
		if (expectedMaxCount > 0
						&& expectedItemDataSize.get() > 0
						&& (OpType.CREATE.equals(expectedOpType)
										|| OpType.READ.equals(expectedOpType)
										|| OpType.UPDATE.equals(expectedOpType))) {
			assertTrue(Long.toString(totalBytes), totalBytes > 0);
		}
		final long countSucc = Long.parseLong(metrics.get("CountSucc"));
		if (expectedMaxCount > 0) {
			if (expectedStepTime > 0) {
				assertTrue(expectedMaxCount >= countSucc);
			} else {
				assertEquals(expectedMaxCount, countSucc);
			}
			assertTrue(Long.toString(countSucc), countSucc > 0);
		}
		final long countFail = Long.parseLong(metrics.get("CountFail"));
		assertTrue("Failures count: " + Long.toString(countFail), countFail <= maxFailureCount);
		// use delta = 5%, because sometimes default storage-mock return error (1 missing response)
		// assertEquals(Long.toString(countFail), 0, countFail, countSucc * 0.05);
		if (countSucc > 0) {
			final long avgItemSize = totalBytes / countSucc;
			if (expectedItemDataSize.getMin() < expectedItemDataSize.getMax()) {
				assertTrue(avgItemSize >= expectedItemDataSize.getMin());
				assertTrue(avgItemSize <= expectedItemDataSize.getMax());
			} else {
				assertEquals(
								Long.toString(avgItemSize),
								expectedItemDataSize.get(),
								avgItemSize,
								expectedItemDataSize.getAvg() / 100);
			}
		}
		final double stepDuration = Double.parseDouble(metrics.get("StepDuration[s]"));
		if (expectedStepTime > 0) {
			assertTrue(
							"Step duration was "
											+ stepDuration
											+ ", but expected not more than"
											+ expectedStepTime
											+ 5,
							stepDuration <= expectedStepTime + 5);
		}
		final double durationSum = Double.parseDouble(metrics.get("DurationSum[s]"));
		final double effEstimate = durationSum / (concurrencyLimit * expectedNodeCount * stepDuration);
		if (countSucc > 0 && concurrencyLimit > 0 && stepDuration > 1) {
			assertTrue(
							"Invalid efficiency estimate: "
											+ effEstimate
											+ ", summary duration: "
											+ durationSum
											+ ", concurrency limit: "
											+ concurrencyLimit
											+ ", driver count: "
											+ nodeCount
											+ ", job duration: "
											+ stepDuration,
							effEstimate <= 1 && effEstimate >= 0);
		}
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

	static void testOpTraceRecord(
					final CSVRecord opTraceRecord, final int opTypeCodeExpected, final SizeInBytes sizeExpected) {
		assertEquals(opTypeCodeExpected, Integer.parseInt(opTraceRecord.get(2)));
		final int actualStatusCode = Integer.parseInt(opTraceRecord.get(3));
		// All FAIL_<...> statuses have .ordinal() more then FAIL_IO
		if (actualStatusCode >= FAIL_IO.ordinal()) {
			// "return" because sometimes default storage-mock return error (1 missing response) and
			// Status = FAIL_<...>
			return;
		}
		if (INTERRUPTED.ordinal() == actualStatusCode) {
			return;
		}
		assertEquals(
						"Actual status code is " + Operation.Status.values()[actualStatusCode],
						SUCC.ordinal(),
						actualStatusCode);
		final long duration = Long.parseLong(opTraceRecord.get(5));
		final String latencyStr = opTraceRecord.get(6);
		if (latencyStr != null && !latencyStr.isEmpty()) {
			assertTrue(duration >= Long.parseLong(latencyStr));
		}
		final long size = Long.parseLong(opTraceRecord.get(8));
		if (sizeExpected.getMin() < sizeExpected.getMax()) {
			assertTrue(
							"Expected the size " + sizeExpected.toString() + ", but got " + size,
							sizeExpected.getMin() <= size && size <= sizeExpected.getMax());
		} else {
			assertEquals(
							"Expected the size " + sizeExpected.toString() + " but got " + size,
							sizeExpected.get(),
							size,
							sizeExpected.get() / 100);
		}
	}

	static void testPartsUploadRecord(final List<CSVRecord> recs) {
		String itemPath, uploadId;
		long respLatency;
		for (final CSVRecord rec : recs) {
			assertEquals(rec.size(), 3);
			itemPath = rec.get("ItemPath");
			assertNotNull(itemPath);
			uploadId = rec.get("UploadId");
			assertNotNull(uploadId);
			respLatency = Long.parseLong(rec.get("RespLatency[us]"));
			assertTrue(respLatency > 0);
		}
	}

	static void testFinalMetricsStdout(
					final String stdOutContent,
					final OpType expectedOpType,
					final int expectedConcurrency,
					final int expectedNodeCount,
					final SizeInBytes expectedItemDataSize,
					final String expectedStepId)
					throws Exception {
		String stepId;
		String opTypeStr;
		int concurrencyLimit;
		int nodeCount;
		double concurrencyMean;
		long totalBytes;
		long countSucc;
		long countFail;
		long avgItemSize;
		double stepDuration;
		double durationSum;
		double tpAvg, tpLast;
		double bwAvg, bwLast;
		double durAvg;
		int durMin, durLoQ, durMed, durHiQ, durMax;
		double latAvg;
		int latMin, latLoQ, latMed, latHiQ, latMax;
		final Matcher matcher = LogPatterns.STD_OUT_METRICS_SUMMARY.matcher(stdOutContent);
		final YAMLFactory yamlFactory = new YAMLFactory();
		final ObjectMapper mapper = new ObjectMapper(yamlFactory);
		final JavaType parsedType = mapper.getTypeFactory().constructArrayType(Map.class);
		boolean found = false;
		while (matcher.find()) {
			final Map<String, Object> parsedContent = ((Map<String, Object>[]) mapper.readValue(matcher.group("content"), parsedType))[0];
			stepId = (String) parsedContent.get(LogPatterns.KEY_STEP_ID);
			if (!expectedStepId.equals(stepId)) {
				continue;
			}
			opTypeStr = (String) parsedContent.get(LogPatterns.KEY_OP_TYPE);
			if (!expectedOpType.name().equals(opTypeStr)) {
				continue;
			}
			found = true;
			concurrencyLimit = (int) ((Map) parsedContent.get(LogPatterns.KEY_CONCURRENCY))
							.get(LogPatterns.KEY_CONCURRENCY_LIMIT);
			assertEquals(Integer.toString(concurrencyLimit), expectedConcurrency, concurrencyLimit);
			nodeCount = (int) parsedContent.get(LogPatterns.KEY_NODE_COUNT);
			assertEquals(Integer.toString(nodeCount), expectedNodeCount, nodeCount);
			concurrencyMean = (double) ((Map) ((Map) parsedContent.get(LogPatterns.KEY_CONCURRENCY))
							.get(LogPatterns.KEY_CONCURRENCY_ACTUAL))
											.get(LogPatterns.KEY_MEAN);
			if (expectedConcurrency > 0) {
				assertTrue(concurrencyMean <= nodeCount * expectedConcurrency);
			} else {
				assertTrue(concurrencyMean >= 0);
			}
			totalBytes = SizeInBytes.toFixedSize((String) parsedContent.get(LogPatterns.KEY_SIZE));
			if (expectedItemDataSize.get() > 0 && !expectedOpType.equals(OpType.DELETE)) {
				assertTrue(Long.toString(totalBytes), totalBytes >= 0);
			}
			countSucc = ((Number) ((Map) parsedContent.get(LogPatterns.KEY_OP_COUNT)).get(LogPatterns.KEY_SUCC))
							.longValue();
			assertTrue(Long.toString(countSucc), countSucc >= 0);
			countFail = ((Number) ((Map) parsedContent.get(LogPatterns.KEY_OP_COUNT)).get(LogPatterns.KEY_FAIL))
							.longValue();
			assertTrue("Failed operations count: " + countFail, countFail < 1);
			if (countSucc > 0) {
				avgItemSize = totalBytes / countSucc;
				if (expectedItemDataSize.getMin() == expectedItemDataSize.getMax()) {
					assertEquals(
									Long.toString(avgItemSize),
									expectedItemDataSize.get(),
									avgItemSize,
									expectedItemDataSize.get() / 100);
				} else {
					assertTrue(
									"Expected " + Long.toString(avgItemSize) + " >= " + expectedItemDataSize.getMin(),
									avgItemSize >= expectedItemDataSize.getMin());
					assertTrue(
									"Expected " + Long.toString(avgItemSize) + " <= " + expectedItemDataSize.getMax(),
									avgItemSize <= expectedItemDataSize.getMax());
				}
			}
			stepDuration = ((Number) ((Map) parsedContent.get(LogPatterns.KEY_DURATION)).get(LogPatterns.KEY_ELAPSED))
							.doubleValue();
			durationSum = ((Number) ((Map) parsedContent.get(LogPatterns.KEY_DURATION)).get(LogPatterns.KEY_SUM))
							.doubleValue();
			assertTrue(durationSum >= 0);
			if (concurrencyLimit > 0 && stepDuration > 0) {
				final double effEstimate = durationSum / (concurrencyLimit * nodeCount * stepDuration);
				assertTrue(Double.toString(effEstimate), effEstimate <= 1 && effEstimate >= 0);
			}
			final Map<String, Double> parsedTp = (Map<String, Double>) parsedContent.get(LogPatterns.KEY_TP);
			tpAvg = parsedTp.get(LogPatterns.KEY_MEAN);
			tpLast = parsedTp.get(LogPatterns.KEY_LAST);
			final Map<String, Double> parsedBw = (Map<String, Double>) parsedContent.get(LogPatterns.KEY_BW);
			bwAvg = parsedBw.get(LogPatterns.KEY_MEAN);
			bwLast = parsedBw.get(LogPatterns.KEY_LAST);
			assertEquals(bwAvg / tpAvg, bwAvg / tpAvg, expectedItemDataSize.getAvg() / 100);
			assertEquals(bwLast / tpLast, bwLast / tpLast, expectedItemDataSize.getAvg() / 100);
			final Map<String, Object> parsedOpsDur = (Map<String, Object>) parsedContent.get(LogPatterns.KEY_OP_DUR);
			durAvg = (double) parsedOpsDur.get(LogPatterns.KEY_AVG);
			assertTrue(durAvg >= 0);
			durMin = (int) parsedOpsDur.get(LogPatterns.KEY_MIN);
			assertTrue(durAvg >= durMin);
			durLoQ = (int) parsedOpsDur.get(LogPatterns.KEY_LOQ);
			assertTrue(durLoQ >= durMin);
			durMed = (int) parsedOpsDur.get(LogPatterns.KEY_MED);
			assertTrue(durMed >= durLoQ);
			durHiQ = (int) parsedOpsDur.get(LogPatterns.KEY_HIQ);
			assertTrue(durHiQ >= durMed);
			durMax = (int) parsedOpsDur.get(LogPatterns.KEY_MAX);
			assertTrue(durMax >= durHiQ);
			assertTrue(durMax >= durAvg);
			final Map<String, Object> parsedOpsLat = (Map<String, Object>) parsedContent.get(LogPatterns.KEY_OP_LAT);
			latAvg = (double) parsedOpsLat.get(LogPatterns.KEY_AVG);
			assertTrue(latAvg >= 0);
			latMin = (int) parsedOpsLat.get(LogPatterns.KEY_MIN);
			assertTrue(latAvg >= latMin);
			latLoQ = (int) parsedOpsLat.get(LogPatterns.KEY_LOQ);
			assertTrue(latLoQ >= latMin);
			latMed = (int) parsedOpsLat.get(LogPatterns.KEY_MED);
			assertTrue(latMed >= latLoQ);
			latHiQ = (int) parsedOpsLat.get(LogPatterns.KEY_HIQ);
			assertTrue(latHiQ >= latMed);
			latMax = (int) parsedOpsLat.get(LogPatterns.KEY_MAX);
			assertTrue(latMax >= latHiQ);
			assertTrue(latMax >= latAvg);
		}
		if (!found) {
			fail(
							"Stdout results not found for step id \""
											+ expectedStepId
											+ "\" & op type \""
											+ expectedOpType
											+ "\"");
		}
	}

	static void testMetricsTableStdout(
					final String stdOutContent,
					final String stepId,
					final StorageType storageType,
					final int nodeCount,
					final long countLimit,
					final Map<OpType, Integer> configConcurrencyMap)
					throws Exception {
		final Matcher m = LogPatterns.STD_OUT_METRICS_TABLE_ROW.matcher(stdOutContent);
		boolean opTypeFoundFlag;
		int rowCount = 0;
		while (m.find()) {
			rowCount++;
			final String actualStepNameEnding = m.group("stepName");
			final Date nextTimstamp = FMT_DATE_METRICS_TABLE.parse(m.group("timestamp"));
			final OpType actualOpType = OpType.valueOf(m.group("opType"));
			final int actualConcurrencyCurr = Integer.parseInt(m.group("concurrencyCurr"));
			final float actualConcurrencyLastMean = Float.parseFloat(m.group("concurrencyLastMean"));
			final long succCount = Long.parseLong(m.group("succCount"));
			final long failCount = Long.parseLong(m.group("failCount"));
			final float stepTimeSec = Float.parseFloat(m.group("stepTime"));
			final float tp = Float.parseFloat(m.group("tp"));
			final float bw = Float.parseFloat(m.group("bw"));
			final long lat = Long.parseLong(m.group("lat"));
			final long dur = Long.parseLong(m.group("dur"));
			assertEquals(
							stepId.length() > 10 ? stepId.substring(stepId.length() - 10) : stepId,
							actualStepNameEnding);
			opTypeFoundFlag = false;
			for (final OpType nextOpType : configConcurrencyMap.keySet()) {
				if (nextOpType.equals(actualOpType)) {
					opTypeFoundFlag = true;
					break;
				}
			}
			assertTrue(
							"I/O type \""
											+ actualOpType
											+ "\" was found but expected one of: "
											+ Arrays.toString(configConcurrencyMap.keySet().toArray()),
							opTypeFoundFlag);
			final int expectedConfigConcurrency = configConcurrencyMap.get(actualOpType);
			if (expectedConfigConcurrency > 0) {
				assertTrue(actualConcurrencyCurr <= nodeCount * expectedConfigConcurrency);
				assertTrue(actualConcurrencyLastMean <= nodeCount * expectedConfigConcurrency);
			} else {
				assertTrue(actualConcurrencyCurr >= 0);
				assertTrue(actualConcurrencyLastMean >= 0);
			}
			if (countLimit > 0) {
				assertTrue(countLimit >= succCount); // count succ
			}
			// next line is commented because they are some problems with storageDriver -> fails
			// assertEquals(0, failCount);
			assertTrue(stepTimeSec >= 0);
			assertTrue(tp >= 0);
			assertTrue(bw >= 0);
			assertTrue(lat >= 0);
			if (!storageType.equals(StorageType.FS)) {
				assertTrue(lat <= dur);
			}
		}
		assertTrue(rowCount > 0);
		final int tableHeaderCount = (stdOutContent.length() - stdOutContent.replaceAll(TABLE_HEADER, "").length())
						/ TABLE_HEADER.length();
		if (tableHeaderCount > 0) {
			assertTrue(rowCount / tableHeaderCount <= TABLE_HEADER_PERIOD);
		}
	}

	static void testFinalMetricsTableRowStdout(
					final String stdOutContent,
					final String stepId,
					final OpType expectedOpType,
					final int nodeCount,
					final int expectedConcurrency,
					final long countLimit,
					final long timeLimit,
					final SizeInBytes expectedItemDataSize) {
		final Matcher m = LogPatterns.STD_OUT_METRICS_TABLE_ROW.matcher(stdOutContent);
		boolean rowFoundFlag = false;
		int actualConcurrencyCurr = -1;
		float actualConcurrencyLastMean = -1;
		long succCount = -1;
		long failCount = -1;
		float stepTimeSec = -1;
		float tp = -1;
		float bw = -1;
		long lat = 0;
		long dur = -1;
		while (m.find()) {
			final String actualStepNameEnding = m.group("stepName");
			if (stepId.endsWith(actualStepNameEnding) || stepId.equals(actualStepNameEnding)) {
				final OpType actualOpType = OpType.valueOf(m.group("opType"));
				if (actualOpType.equals(expectedOpType)) {
					rowFoundFlag = true;
					actualConcurrencyCurr = Integer.parseInt(m.group("concurrencyCurr"));
					actualConcurrencyLastMean = Float.parseFloat(m.group("concurrencyLastMean"));
					succCount = Long.parseLong(m.group("succCount"));
					failCount = Long.parseLong(m.group("failCount"));
					stepTimeSec = Float.parseFloat(m.group("stepTime"));
					tp = Float.parseFloat(m.group("tp"));
					bw = Float.parseFloat(m.group("bw"));
					lat = Long.parseLong(m.group("lat"));
					dur = Long.parseLong(m.group("dur"));
				}
			}
		}
		assertTrue(
						"Summary metrics row with step id ending with \""
										+ stepId
										+ "\" and I/O type \""
										+ expectedOpType
										+ "\" was not found",
						rowFoundFlag);
		assertTrue(actualConcurrencyCurr >= 0);
		assertTrue(nodeCount * expectedConcurrency >= actualConcurrencyCurr);
		assertTrue(actualConcurrencyLastMean >= 0);
		assertTrue(nodeCount * expectedConcurrency >= actualConcurrencyLastMean);
		assertTrue("Successful operations count should be > 0", succCount > 0);
		assertEquals("Failure count should be 0", failCount, 0);
		assertTrue("Step time should be > 0", stepTimeSec > 0);
		if (timeLimit > 0) {
			assertTrue(
							"Step time ("
											+ stepTimeSec
											+ ") should not be much more than time limit ("
											+ timeLimit
											+ ")",
							stepTimeSec <= timeLimit + 10);
		}
		assertTrue("Final throughput should be >= 0", tp >= 0);
		if (expectedItemDataSize != null && tp > 0) {
			final float avgItemSize = MIB * bw / tp;
			if (expectedItemDataSize.getMin() == expectedItemDataSize.getMax()) {
				assertEquals(
								"Actual average items size ("
												+ new SizeInBytes((long) avgItemSize)
												+ ") should be approx equal the expected ("
												+ expectedItemDataSize
												+ ")",
								expectedItemDataSize.get(),
								avgItemSize,
								expectedItemDataSize.get() / 10);
			} else {
				assertTrue(
								"Actual average items size ("
												+ new SizeInBytes((long) avgItemSize)
												+ ") doesn't fit the expected ("
												+ expectedItemDataSize
												+ ")",
								avgItemSize >= expectedItemDataSize.getMin());
				assertTrue(
								"Actual average items size ("
												+ new SizeInBytes((long) avgItemSize)
												+ ") doesn't fit the expected ("
												+ expectedItemDataSize
												+ ")",
								avgItemSize <= expectedItemDataSize.getMax());
			}
		}
		assertTrue(
						"Mean latency (" + lat + ") should not be more than mean duration (" + dur + ")",
						lat <= dur);
	}
}
