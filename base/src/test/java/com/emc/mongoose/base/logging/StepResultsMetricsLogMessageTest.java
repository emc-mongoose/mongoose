package com.emc.mongoose.base.logging;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.Constants;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.metrics.MetricsConstants;
import com.emc.mongoose.base.metrics.snapshot.ConcurrencyMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.DistributedAllMetricsSnapshot;
import com.emc.mongoose.base.metrics.snapshot.DistributedAllMetricsSnapshotImpl;
import com.emc.mongoose.base.metrics.snapshot.HistogramSnapshot;
import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshot;
import com.emc.mongoose.base.metrics.snapshot.RateMetricSnapshotImpl;
import com.emc.mongoose.base.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.base.metrics.type.ConcurrencyMeterImpl;
import com.emc.mongoose.base.metrics.type.HistogramImpl;
import com.emc.mongoose.base.metrics.type.LongMeter;
import com.emc.mongoose.base.metrics.type.TimingMeterImpl;
import com.emc.mongoose.base.metrics.util.ConcurrentSlidingWindowLongReservoir;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.util.Map;
import java.util.stream.LongStream;
import org.junit.Test;

public class StepResultsMetricsLogMessageTest extends StepResultsMetricsLogMessage {

	private static final OpType OP_TYPE = OpType.READ;
	private static final String STEP_ID = StepResultsMetricsLogMessageTest.class.getSimpleName();
	private static final int COUNT = 123456;
	private static final int DUR_MAX = 31416;
	private static final int LAT_MAX = 27183;
	private static final long[] DURATIONS = new long[COUNT];
	private static long durSum = 0;

	static {
		for (int i = 0; i < COUNT; i++) {
			DURATIONS[i] = System.nanoTime() % DUR_MAX;
			durSum += DURATIONS[i];
		}
	}

	private static final long[] LATENCIES = new long[COUNT];
	private static long latSum = 0;

	static {
		for (int i = 0; i < COUNT; i++) {
			LATENCIES[i] = System.nanoTime() % LAT_MAX;
			latSum += LATENCIES[i];
		}
	}

	private static final long[] CONCURRENCIES = new long[COUNT];

	static {
		for (int i = 0; i < COUNT; i++) {
			CONCURRENCIES[i] = 10;
		}
	}

	private static final DistributedAllMetricsSnapshot SNAPSHOT;

	static {
		LongMeter<HistogramSnapshot> h;
		h = new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(COUNT));
		LongStream.of(DURATIONS).forEach(h::update);
		final TimingMetricSnapshot dS = new TimingMeterImpl(h, MetricsConstants.METRIC_NAME_DUR).snapshot();
		h = new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(COUNT));
		LongStream.of(LATENCIES).forEach(h::update);
		final TimingMetricSnapshot lS = new TimingMeterImpl(h, MetricsConstants.METRIC_NAME_LAT).snapshot();
		h = new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(COUNT));
		LongStream.of(CONCURRENCIES).forEach(h::update);
		final ConcurrencyMetricSnapshot cS = new ConcurrencyMeterImpl(MetricsConstants.METRIC_NAME_CONC).snapshot();
		final RateMetricSnapshot fS = new RateMetricSnapshotImpl(0, 0, MetricsConstants.METRIC_NAME_FAIL, 0, 0);
		final RateMetricSnapshot sS = new RateMetricSnapshotImpl(
						COUNT / durSum, COUNT / durSum, MetricsConstants.METRIC_NAME_SUCC, COUNT, 0);
		final RateMetricSnapshot bS = new RateMetricSnapshotImpl(
						COUNT / durSum,
						COUNT / durSum,
						MetricsConstants.METRIC_NAME_BYTE,
						new Double(COUNT * Constants.K).longValue(),
						0);
		SNAPSHOT = new DistributedAllMetricsSnapshotImpl(dS, lS, cS, fS, sS, bS, 2, 123456);
	}

	public StepResultsMetricsLogMessageTest() {
		super(OP_TYPE, STEP_ID, 0, SNAPSHOT);
	}

	@Test
	public final void testIsValidYaml() throws Exception {
		final StringBuilder buff = new StringBuilder();
		formatTo(buff);
		System.out.println(buff.toString());
		final YAMLFactory yamlFactory = new YAMLFactory();
		final ObjectMapper mapper = new ObjectMapper(yamlFactory);
		final JavaType parsedType = mapper.getTypeFactory().constructArrayType(Map.class);
		final Map<String, Object> parsed = ((Map<String, Object>[]) mapper.readValue(buff.toString(), parsedType))[0];
		assertEquals(STEP_ID, parsed.get("Load Step Id"));
		assertEquals(OP_TYPE.name(), parsed.get("Operation Type"));
		assertEquals(COUNT, ((Map<String, Object>) parsed.get("Operations Count")).get("Successful"));
	}
}
