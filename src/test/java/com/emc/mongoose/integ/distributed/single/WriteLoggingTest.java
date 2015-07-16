package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import static com.emc.mongoose.integ.tools.LogPatterns.*;
import com.emc.mongoose.integ.tools.SavedOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 16.07.15.
 */
public class WriteLoggingTest {
	//
	private final static long COUNT_TO_WRITE = 100000;
	//
	private static StorageClient<WSObject> CLIENT;
	private static long COUNT_WRITTEN;
	private static Logger LOG;
	private static byte STD_OUT_CONTENT[];
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		CLIENT = clientBuilder
			.setLimitTime(0, TimeUnit.SECONDS)
			.setLimitCount(COUNT_TO_WRITE)
			.setClientMode(new String[] {ServiceUtils.getHostAddr()})
			.build();
		final SavedOutputStream
			stdOutInterceptorStream = StdOutInterceptorTestSuite.STD_OUT_INTERCEPT_STREAM;
		if(stdOutInterceptorStream == null) {
			throw new IllegalStateException(
				"Looks like the test case is not included in the \"" +
				StdOutInterceptorTestSuite.class.getSimpleName() + "\" test suite, cannot run"
			);
		}
		stdOutInterceptorStream.reset(); // clear before using
		COUNT_WRITTEN = CLIENT.write(null, null, (short) 10, SizeUtil.toSize("10KB"));
		TimeUnit.SECONDS.sleep(1);
		STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
		LOG = LogManager.getLogger();
		LOG.info(
			Markers.MSG, "Written {} items, captured {} bytes from stdout",
			COUNT_WRITTEN, STD_OUT_CONTENT.length
		);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CLIENT.close();
	}
	//
	@Test
	public void checkConsoleAvgMetricsLogging()
	throws Exception {
		try(
			final BufferedReader in = new BufferedReader(
				new InputStreamReader(new ByteArrayInputStream(STD_OUT_CONTENT))
			)
		) {
			String nextStdOutLine;
			Matcher nextLineMatcher;
			do {
				nextStdOutLine = in.readLine();
				if(nextStdOutLine == null) {
					break;
				} else {
					nextLineMatcher = CONSOLE_METRICS_AVG.matcher(nextStdOutLine);
					if(nextLineMatcher.find()) {
						LOG.warn(Markers.MSG, nextLineMatcher.group("dateTime"));
					} else {
						LOG.error(
							Markers.MSG, "Line \"{}\" doesn't match the pattern \"{}\"",
							nextStdOutLine, nextLineMatcher.pattern()
						);
					}
				}
			} while(true);
		}
	}
	//
	@Test
	public void checkConsoleSumMetricsLogging() {
		final Matcher matcher = CONSOLE_METRICS_SUM_CLIENT.matcher(new String(STD_OUT_CONTENT));
	}
	//
	@Test
	public void checkFileAvgMetricsLogging() {

	}
	//
	@Test
	public void checkFileSumMetricsLogging() {

	}
}
