package com.emc.mongoose.base.control.logs;

import static com.emc.mongoose.base.Constants.MIB;
import static org.eclipse.jetty.server.InclusiveByteRange.satisfiableRanges;

import com.emc.mongoose.base.Constants;
import com.emc.mongoose.base.logging.Loggers;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.RollingRandomAccessFileAppender;
import org.apache.logging.log4j.core.async.AsyncLogger;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.InclusiveByteRange;

public final class LogServlet extends HttpServlet {

	private static final String KEY_STEP_ID = "stepId";
	private static final String KEY_LOGGER_NAME = "loggerName";
	private static final Pattern PATTERN_URI_PATH = Pattern.compile(
					"/logs/(?<" + KEY_STEP_ID + ">[\\w\\-_.,;:~=+@]+)/(?<" + KEY_LOGGER_NAME + ">[\\w_.]+)");
	private static final String PATTERN_STEP_ID_SUBST = "${ctx:" + Constants.KEY_STEP_ID + "}";
	private static final int LOG_PAGE_SIZE_LIMIT = MIB;

	private final Map<String, String> logFileNamePatternByName;

	public LogServlet() {
		logFileNamePatternByName = Arrays.stream(Loggers.class.getFields())
						.map(
										field -> {
											try {
												return field.get(null);
											} catch (final Exception e) {
												throw new AssertionError(e);
											}
										})
						.filter(fieldVal -> fieldVal instanceof Logger)
						.map(o -> (Logger) o)
						.filter(logger -> logger.getName().startsWith(Loggers.BASE))
						.collect(
										Collectors.toMap(
														logger -> logger.getName().substring(Loggers.BASE.length()),
														logger -> ((AsyncLogger) logger)
																		.getAppenders().values().stream()
																		.filter(
																						appender -> appender instanceof RollingRandomAccessFileAppender)
																		.map(
																						appender -> ((RollingRandomAccessFileAppender) appender)
																										.getFilePattern())
																		.findAny()
																		.orElse("")));
	}

	@Override
	protected final void doGet(final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException {
		try {
			final Path logFilePath = logFilePath(req);
			try {
				respondFileContent(logFilePath, req, resp);
				resp.setStatus(HttpStatus.OK_200);
			} catch (final MultipleByteRangesException e) {
				resp.sendError(HttpStatus.RANGE_NOT_SATISFIABLE_416, e.getMessage());
			}
		} catch (final NoLoggerException | InvalidUriPathException e) {
			resp.sendError(HttpStatus.BAD_REQUEST_400, e.getMessage());
		} catch (final NoLogFileException e) {
			resp.sendError(HttpStatus.NOT_FOUND_404, e.getMessage());
		}
	}

	@Override
	protected final void doDelete(final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException {
		try {
			final Path logFilePath = logFilePath(req);
			Files.delete(logFilePath);
			resp.setStatus(HttpStatus.OK_200);
		} catch (final NoLoggerException | InvalidUriPathException e) {
			resp.sendError(HttpStatus.BAD_REQUEST_400, e.getMessage());
		} catch (final NoLogFileException e) {
			resp.sendError(HttpStatus.NOT_FOUND_404, e.getMessage());
		}
	}

	private Path logFilePath(final HttpServletRequest req)
					throws NoLoggerException, NoLogFileException, InvalidUriPathException {
		final String reqUri = req.getRequestURI();
		final Matcher matcher = PATTERN_URI_PATH.matcher(reqUri);
		if (matcher.find()) {
			final String stepId = matcher.group(KEY_STEP_ID);
			final String loggerName = matcher.group(KEY_LOGGER_NAME);
			final String logFileNamePattern = logFileNamePatternByName.get(loggerName);
			if (null == logFileNamePattern) {
				throw new NoLoggerException("No such logger: \"" + loggerName + "\"");
			} else if (logFileNamePattern.isEmpty()) {
				throw new NoLogFileException(
								"Unable to determine the log file for the logger \"" + loggerName + "\"");
			} else {
				final String logFile = logFileNamePattern.replace(PATTERN_STEP_ID_SUBST, stepId);
				return Paths.get(logFile);
			}
		} else {
			throw new InvalidUriPathException(
							"Unable to extract a step id/logger name from the URI path: \"" + reqUri + "\"");
		}
	}

	private static void respondFileContent(
					final Path filePath, final HttpServletRequest req, final HttpServletResponse resp)
					throws IOException, MultipleByteRangesException, NoLogFileException {
		if (Files.exists(filePath)) {
			final Enumeration<String> rangeHeaders = req.getHeaders(HttpHeader.RANGE.asString());
			final List<InclusiveByteRange> byteRanges = satisfiableRanges(rangeHeaders, Files.size(filePath));
			final long offset;
			final long size;
			if (byteRanges == null || 0 == byteRanges.size()) {
				offset = 0;
				size = LOG_PAGE_SIZE_LIMIT - 1;
			} else if (1 == byteRanges.size()) {
				final InclusiveByteRange byteRange = byteRanges.get(0);
				offset = byteRange.getFirst();
				size = byteRange.getLast() + 1;
			} else {
				throw new MultipleByteRangesException("Unable to process more than 1 range header");
			}
			writeFileRange(filePath, offset, size, resp.getOutputStream(), resp.getBufferSize());
		} else {
			throw new NoLogFileException("The log file doesn't exist");
		}
	}

	private static void writeFileRange(
					final Path filePath,
					final long offset,
					final long size,
					final OutputStream out,
					final int buffSize)
					throws IOException {
		final int sizeLimit = (int) Math.min(size, LOG_PAGE_SIZE_LIMIT);
		try (final InputStream in = Files.newInputStream(filePath)) {

			long skipped = 0;
			while (offset > skipped) {
				skipped += in.skip(offset - skipped);
			}

			final byte[] buff = new byte[Math.min(sizeLimit, buffSize)];
			long done = 0;
			int n;
			while (done < sizeLimit && -1 != (n = in.read(buff))) {
				out.write(buff, 0, n);
				done += n;
			}
		}
	}
}
