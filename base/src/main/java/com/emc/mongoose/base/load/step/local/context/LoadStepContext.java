package com.emc.mongoose.base.load.step.local.context;

import com.emc.mongoose.base.concurrent.Daemon;
import com.emc.mongoose.base.item.Item;
import com.emc.mongoose.base.item.op.Operation;
import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import java.io.IOException;

/** Created on 11.07.16. */
public interface LoadStepContext<I extends Item, O extends Operation<I>> extends Daemon, Output<O> {

	void operationsResultsOutput(final Output<O> opsResultsOutput);

	int activeOpCount();

	boolean isDone();

	default Input<O> getInput() {
		throw new AssertionError("Shouldn't be invoked");
	}

	@Override
	AsyncRunnable stop() ;

	@Override
	void close() throws IOException;
}
