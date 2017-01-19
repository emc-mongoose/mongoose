package com.emc.mongoose.tests.system.base;

import com.emc.mongoose.common.env.PathUtil;
import com.emc.mongoose.ui.log.LogUtil;
import static com.emc.mongoose.common.Constants.KEY_JOB_NAME;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 Created by andrey on 19.01.17.
 */
public abstract class LoggingTestBase {

	protected static Logger LOG;
	protected static String JOB_NAME;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LogUtil.init();
		JOB_NAME = ThreadContext.get(KEY_JOB_NAME);
		LOG = LogManager.getLogger();
	}

	private static List<String> getLogFileLines(final String fileName)
	throws IOException {
		final File logFile = Paths.get(PathUtil.getBaseDir(), "log", fileName).toFile();
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

	protected static List<String> getPartsUploadLongLines()
	throws IOException {
		return getLogFileLines("parts.upload.log");
	}

	private static List<CSVRecord> getLogFileCsvRecords(final String fileName)
	throws IOException {
		final File logFile = Paths.get(PathUtil.getBaseDir(), "log", fileName).toFile();
		try(final BufferedReader br = new BufferedReader(new FileReader(logFile))) {
			final CSVParser csvParser = CSVFormat.RFC4180.parse(br);
			final List<CSVRecord> csvRecords = new ArrayList<>();
			for(final CSVRecord csvRecord : csvParser) {
				csvRecords.add(csvRecord);
			}
			return csvRecords;
		}
	}

	private static List<CSVRecord> getMetricsMedLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.med.csv");
	}

	private static List<CSVRecord> getMetricsMedTotalLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.med.total.csv");
	}

	private static List<CSVRecord> getMetricsLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.csv");
	}

	private static List<CSVRecord> getMetricsTotalLogRecords()
	throws IOException {
		return getLogFileCsvRecords("metrics.total.csv");
	}

	private static List<CSVRecord> getIoTraceLogRecords()
	throws IOException {
		return getLogFileCsvRecords("io.trace.csv");
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LogUtil.shutdown();
	}
}
