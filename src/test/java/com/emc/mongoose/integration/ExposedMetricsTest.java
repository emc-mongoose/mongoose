package com.emc.mongoose.integration;

import com.emc.mongoose.Constants;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.metrics.MetricsManagerImpl;
import com.emc.mongoose.metrics.snapshot.MetricsSnapshot;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;
import com.emc.mongoose.metrics.context.DistributedMetricsContextImpl;
import com.emc.mongoose.metrics.context.MetricsContext;
import com.emc.mongoose.metrics.context.MetricsContextImpl;
import com.emc.mongoose.params.ItemSize;
import com.github.akurilov.commons.system.SizeInBytes;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 @author veronika K. on 15.10.18 */
public class ExposedMetricsTest {

	private static final int PORT = 1111;
	private static final String CONTEXT = "/metrics";
	private static final int ITERATION_COUNT = 10;
	private static final Double TIMING_ACCURACY = 0.0001;
	private static final Double RATE_ACCURACY = 0.2;
	private static final int MARK_DUR = 1_100_000; //dur must be more than lat (dur > lat)
	private static final int MARK_LAT = 1_000_000;
	private static final String[] TIMING_METRICS = { "count", "sum", "mean", "min", "max" };
	private static final String[] RATE_METRICS = { "count", "meanRate", "lastRate" };
	private final String STEP_ID = ExposedMetricsTest.class.getSimpleName();
	private final OpType OP_TYPE = OpType.CREATE;
	private final IntSupplier nodeCountSupplier = () -> 1;
	private final int concurrencyLimit = 0;
	private final int concurrencyThreshold = 0;
	private final SizeInBytes ITEM_DATA_SIZE = ItemSize.SMALL.getValue();
	private final int UPDATE_INTERVAL_SEC = (int) TimeUnit.MICROSECONDS.toSeconds(MARK_DUR);
	private Supplier<List<MetricsSnapshot>> snapshotsSupplier;
	private final Server server = new Server(PORT);
	//
	private DistributedMetricsContext distributedMetricsContext;
	private MetricsContext metricsContext;

	@Before
	public void setUp()
	throws Exception {
		//
		final ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new MetricsServlet()), CONTEXT);
		server.start();
		//
		metricsContext = new MetricsContextImpl<>(
			STEP_ID, OP_TYPE, () -> 1, concurrencyLimit, concurrencyThreshold, ITEM_DATA_SIZE, UPDATE_INTERVAL_SEC, true
		);
		snapshotsSupplier = () -> Arrays.asList(metricsContext.lastSnapshot());
		metricsContext.start();
		//
		distributedMetricsContext = new DistributedMetricsContextImpl(
			STEP_ID, OP_TYPE, nodeCountSupplier, concurrencyLimit, concurrencyThreshold, ITEM_DATA_SIZE,
			UPDATE_INTERVAL_SEC, true, true, true, true, snapshotsSupplier
		);
		distributedMetricsContext.start();
	}

	@Test
	public void test()
	throws Exception {
		final MetricsManager metricsMgr = new MetricsManagerImpl(ServiceTaskExecutor.INSTANCE);
		metricsMgr.register(distributedMetricsContext);
		for(int i = 0; i < ITERATION_COUNT; ++ i) {
			metricsContext.markSucc(ITEM_DATA_SIZE.get(), MARK_DUR, MARK_LAT);
			metricsContext.markFail();
			metricsContext.refreshLastSnapshot();
			TimeUnit.MICROSECONDS.sleep(MARK_DUR);
		}
		final String result = resultFromServer("http://localhost:" + PORT + CONTEXT);
		System.out.println(result);
		//
		final Map tmp = new HashMap();
		final long elapsedTimeMillis = TimeUnit.MICROSECONDS.toMillis(MARK_DUR * ITERATION_COUNT);
		tmp.put("value", new Double(elapsedTimeMillis));
		testMetric(result, Constants.METRIC_NAME_TIME, tmp, RATE_ACCURACY);
		//
		testTimingMetric(result, MARK_DUR, Constants.METRIC_NAME_DUR);
		testTimingMetric(result, MARK_LAT, Constants.METRIC_NAME_LAT);
		testTimingMetric(result, nodeCountSupplier.getAsInt(), Constants.METRIC_NAME_CONC);
		//
		testRateMetric(result, ITEM_DATA_SIZE.get(), Constants.METRIC_NAME_BYTE);
		testRateMetric(result, 1, Constants.METRIC_NAME_FAIL);
		testRateMetric(result, 1, Constants.METRIC_NAME_SUCC);
		//
		metricsMgr.close();
	}

	private void testTimingMetric(final String stdOut, final double markValue, final String name) {
		final Map<String, Double> expectedValues = new HashMap<>();
		// concurrency count != iteration_count, because in the refreshLastSnapshot lat & dur account only after the condition, and concurrency - every time
		final double count = name.equals(Constants.METRIC_NAME_CONC) ?
							 ITERATION_COUNT + 1 : ITERATION_COUNT;
		final double accuracy = name.equals(Constants.METRIC_NAME_CONC) ?
								RATE_ACCURACY : TIMING_ACCURACY;
		final double[] values = { count, markValue * count, markValue, markValue, markValue };
		for(int i = 0; i < TIMING_METRICS.length; ++ i) {
			expectedValues.put(TIMING_METRICS[i], values[i]);
		}
		testMetric(stdOut, name, expectedValues, accuracy);
	}

	private void testRateMetric(final String stdOut, final double markValue, final String name) {
		final Map<String, Double> expectedValues = new HashMap<>();
		double count = ITERATION_COUNT;
		if(name.equals(Constants.METRIC_NAME_BYTE)) {
			count *= markValue;
		}
		final Double[] values = { count, markValue, markValue };
		for(int i = 0; i < RATE_METRICS.length; ++ i) {
			expectedValues.put(RATE_METRICS[i], values[i]);
		}
		testMetric(stdOut, name, expectedValues, RATE_ACCURACY);
	}

	private String resultFromServer(final String urlPath)
	throws Exception {
		final StringBuilder stringBuilder = new StringBuilder();
		final URL url = new URL(urlPath);
		final URLConnection conn = url.openConnection();
		try(final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
			br.lines().forEach(l -> stringBuilder.append(l).append("\n"));
		}
		return stringBuilder.toString();
	}

	private void testMetric(
		final String resultOutput, final String metricName, final Map<String, Double> expectedValues,
		final double accuracy
	) {
		for(final String key : expectedValues.keySet()) {
			final Pattern p = Pattern.compile(metricName + "_" + key + "\\{.+\\} .+");
			final Matcher m = p.matcher(resultOutput);
			Assert.assertEquals(m.find(), true);
			final Double actualValue = Double.valueOf(m.group().split("}")[1]);
			final Double expectedValue = Double.valueOf(expectedValues.get(key));
			Assert.assertEquals(
				"metric : " + metricName + "_" + key, expectedValue, actualValue, expectedValue * accuracy
			);
		}
	}
}