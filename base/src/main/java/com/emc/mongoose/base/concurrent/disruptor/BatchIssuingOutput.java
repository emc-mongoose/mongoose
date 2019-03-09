package com.emc.mongoose.base.concurrent.disruptor;

import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.commons.io.Output;

import java.util.List;
import java.util.function.Consumer;

public interface BatchIssuingOutput<T>
				extends AsyncRunnable, Output<T> {

	int remaining();

	void register(final Consumer<List<T>> batchHandler);
}
