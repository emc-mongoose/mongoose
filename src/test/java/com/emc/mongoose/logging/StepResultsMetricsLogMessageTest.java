package com.emc.mongoose.logging;

import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.metrics.DistributedMetricsSnapshot;
import com.emc.mongoose.metrics.DistributedMetricsSnapshotImpl;

public class StepResultsMetricsLogMessageTest
extends StepResultsMetricsLogMessage {

	private static final OpType OP_TYPE = OpType.READ;
	private static final boolean STD_OUT_COLOR_FLAG = true;
	private static final String STEP_ID = StepResultsMetricsLogMessageTest.class.getSimpleName();
	private static final DistributedMetricsSnapshot SNAPSHOT = new DistributedMetricsSnapshotImpl(

	);

	public StepResultsMetricsLogMessageTest() {
		super(OP_TYPE, STD_OUT_COLOR_FLAG, STEP_ID, SNAPSHOT);
	}
}