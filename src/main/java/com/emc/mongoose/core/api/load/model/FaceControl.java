package com.emc.mongoose.core.api.load.model;
//
import java.util.List;
/**
 Created by kurila on 29.03.16.
 */
public interface FaceControl<X> {

	boolean requestApprovalFor(final X thing)
	throws InterruptedException;

	boolean requestBatchApprovalFor(final List<X> things, int from, int to)
	throws InterruptedException;
}
