package com.emc.mongoose.logging;


import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.DistributedMetricsSnapshotImpl;
import com.emc.mongoose.metrics.HistogramSnapshotImpl;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;


public class StepResultsMetricsLogMessageTest
extends StepResultsMetricsLogMessage {

	private static final OpType OP_TYPE = OpType.READ;
	private static final String STEP_ID = StepResultsMetricsLogMessageTest.class.getSimpleName();
	private static final int COUNT = 123456;
	private static final int DUR_MAX = 31416;
	private static final int LAT_MAX = 27183;

	private static final long[] DURATIONS = new long[COUNT];
	private static long durSum = 0;
	static {
		for(int i = 0; i < COUNT; i ++) {
			DURATIONS[i] = System.nanoTime() % DUR_MAX;
			durSum += DURATIONS[i];
		}
	}

	private static final long[] LATENCIES = new long[COUNT];
	private static long latSum = 0;
	static {
		for(int i = 0; i < COUNT; i ++) {
			LATENCIES[i] = System.nanoTime() % LAT_MAX;
			latSum += LATENCIES[i];
		}
	}

	private static final DistributedMetricsSnapshot SNAPSHOT = new DistributedMetricsSnapshotImpl(
		COUNT, 789, 123, 4.56, 7890123, 4567, 1234567890,
		123456, 456789, 7.89, 10
		, 2, new HistogramSnapshotImpl(DURATIONS), new HistogramSnapshotImpl(LATENCIES)
	);

	public StepResultsMetricsLogMessageTest() {
		super(OP_TYPE, STEP_ID, SNAPSHOT);
	}

	@Test
	public final void testIsValidYaml()
	throws Exception {
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
