package com.emc.mongoose.integration;

import com.emc.mongoose.Constants;
import com.emc.mongoose.concurrent.ServiceTaskExecutor;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.MetricsConstants;
import com.emc.mongoose.metrics.MetricsManager;
import com.emc.mongoose.metrics.MetricsManagerImpl;
import com.emc.mongoose.metrics.context.DistributedMetricsContext;
import com.emc.mongoose.metrics.context.DistributedMetricsContextImpl;
import com.emc.mongoose.metrics.context.MetricsContext;
import com.emc.mongoose.metrics.context.MetricsContextImpl;
import com.emc.mongoose.metrics.snapshot.AllMetricsSnapshot;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.emc.mongoose.metrics.MetricsConstants.METRIC_FORMAT;

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
	private static final String[] CONCURRENCY_METRICS = { "mean", "last" };
	private static final String[] TIMING_METRICS =
		{ "count", "sum", "mean", "min", "max", "quantile_0.25", "quantile_0.5", "quantile_0.75" };
	private static final String[] OPS_METRICS = { "count", "rate_mean", "rate_last" };
	private static final String[] BYTES_METRICS = { "count", "rate_mean", "rate_last" };
	private static final Double[] QUANTILE_VALUES = { 0.25, 0.5, 0.75 };
	private static final List<String> nodeList = Arrays.asList("127.0.0.1:1099");
	private final String STEP_ID = ExposedMetricsTest.class.getSimpleName();
	private final OpType OP_TYPE = OpType.CREATE;
	private final IntSupplier nodeCountSupplier = () -> 1;
	private final int CONCURRENCY_LIMIT = 0;
	private final int CONCURRENCY_THRESHOLD = 0;
	private final SizeInBytes ITEM_DATA_SIZE = ItemSize.SMALL.getValue();
	private final int UPDATE_INTERVAL_SEC = (int) TimeUnit.MICROSECONDS.toSeconds(MARK_DUR);
	private Supplier<List<AllMetricsSnapshot>> snapshotsSupplier;
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
		metricsContext = MetricsContextImpl
			.builder()
			.id(STEP_ID)
			.opType(OP_TYPE)
			.actualConcurrencyGauge(() -> 1)
			.concurrencyLimit(CONCURRENCY_LIMIT)
			.concurrencyThreshold(CONCURRENCY_THRESHOLD)
			.itemDataSize(ITEM_DATA_SIZE)
			.outputPeriodSec(UPDATE_INTERVAL_SEC)
			.stdOutColorFlag(true)
			.comment("")
			.build();
		snapshotsSupplier = () -> Arrays.asList(metricsContext.lastSnapshot());
		metricsContext.start();
		//
		distributedMetricsContext = DistributedMetricsContextImpl
			.builder()
			.id(STEP_ID)
			.opType(OP_TYPE)
			.nodeCountSupplier(nodeCountSupplier)
			.concurrencyLimit(CONCURRENCY_LIMIT)
			.concurrencyThreshold(CONCURRENCY_THRESHOLD)
			.itemDataSize(ITEM_DATA_SIZE)
			.outputPeriodSec(UPDATE_INTERVAL_SEC)
			.stdOutColorFlag(true)
			.avgPersistFlag(true)
			.sumPersistFlag(true)
			.snapshotsSupplier(snapshotsSupplier)
			.quantileValues(Arrays.asList(QUANTILE_VALUES))
			.nodeAddrs(nodeList)
			.comment("")
			.build();
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
		final long elapsedTimeMillis = TimeUnit.MICROSECONDS.toSeconds(MARK_DUR * ITERATION_COUNT);
		tmp.put("value", new Double(elapsedTimeMillis));
		testMetric(result, MetricsConstants.METRIC_NAME_TIME, tmp, RATE_ACCURACY);
		//
		testTimingMetric(result, MARK_DUR, MetricsConstants.METRIC_NAME_DUR);
		testTimingMetric(result, MARK_LAT, MetricsConstants.METRIC_NAME_LAT);
		testConcurrencyMetric(result, 1, MetricsConstants.METRIC_NAME_CONC);
		//
		testRateMetric(result, ITEM_DATA_SIZE.get(), MetricsConstants.METRIC_NAME_BYTE);
		testRateMetric(result, 1, MetricsConstants.METRIC_NAME_FAIL);
		testRateMetric(result, 1, MetricsConstants.METRIC_NAME_SUCC);
		//
		testLables(result);
		metricsMgr.close();
	}

	private void testLabel(final String result, final String labelName, final String expectedValue) {
		final Pattern p = Pattern.compile("\\{.+" + labelName +"\\} .+");
		final Matcher m = p.matcher(resultOutput);
		final boolean found = m.find();
		Assert.assertTrue(found);
		final Double actualValue = Double.valueOf(m.group().split("}")[1]);
		final Double expectedValue = Double.valueOf(expectedValues.get(key));
		Assert.assertEquals(" : " + metricName + "_" + key, expectedValue, actualValue, expectedValue * accuracy
			);
	}

	private void testTimingMetric(final String stdOut, final double markValue, final String name) {
		final Map<String, Double> expectedValues = new HashMap<>();
		// concurrency count != iteration_count, because in the refreshLastSnapshot lat & dur account only after the condition, and concurrency - every time
		final double count = ITERATION_COUNT;
		final double accuracy = TIMING_ACCURACY;
		final double markValueInSec = markValue / Constants.M;
		final double[] values =
			{
				count, markValueInSec * count, markValueInSec, markValueInSec, markValueInSec, markValueInSec,
				markValueInSec, markValueInSec
			};
		for(int i = 0; i < TIMING_METRICS.length; ++ i) {
			expectedValues.put(TIMING_METRICS[i], values[i]);
		}
		testMetric(stdOut, name, expectedValues, accuracy);
	}

	private void testRateMetric(final String stdOut, final double markValue, final String name) {
		final Map<String, Double> expectedValues = new HashMap<>();
		double count = ITERATION_COUNT;
		String[] rateMetrics = OPS_METRICS;
		if(name.equals(MetricsConstants.METRIC_NAME_BYTE)) {
			count *= markValue;
			rateMetrics = BYTES_METRICS;
		}
		final Double[] values = { count, markValue, markValue };
		for(int i = 0; i < rateMetrics.length; ++ i) {
			expectedValues.put(rateMetrics[i], values[i]);
		}
		testMetric(stdOut, name, expectedValues, RATE_ACCURACY);
	}

	private void testConcurrencyMetric(final String stdOut, final double markValue, final String name) {
		final Map<String, Double> expectedValues = new HashMap<>();
		final double accuracy = 0;
		final double[] values = { 1, 1, };
		for(int i = 0; i < CONCURRENCY_METRICS.length; ++ i) {
			expectedValues.put(CONCURRENCY_METRICS[i], values[i]);
		}
		testMetric(stdOut, name, expectedValues, accuracy);
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
			final Pattern p = Pattern.compile(String.format(METRIC_FORMAT, metricName, key) + "\\{.+\\} .+");
			final Matcher m = p.matcher(resultOutput);
			final boolean found = m.find();
			Assert.assertTrue(found);
			final Double actualValue = Double.valueOf(m.group().split("}")[1]);
			final Double expectedValue = Double.valueOf(expectedValues.get(key));
			Assert.assertEquals(
				"metric : " + metricName + "_" + key, expectedValue, actualValue, expectedValue * accuracy
			);
		}
	}

}
