package com.emc.mongoose.load.monitor;

import com.emc.mongoose.common.concurrent.Throttle;
import com.emc.mongoose.model.io.Input;
import com.emc.mongoose.model.io.Output;
import com.emc.mongoose.model.io.task.IoTask;
import com.emc.mongoose.model.item.Item;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
/**
 Created by andrey on 06.11.16.
 */
public final class NewIoTaskDispatcher<I extends Item, O extends IoTask<I>>
implements Output<O> {

	private final List<Output<O>> outputs;
	private final int count;
	private final AtomicLong rrc = new AtomicLong(0);

	public NewIoTaskDispatcher(final List<Output<O>> outputs) {
		this.outputs = outputs;
		this.count = outputs.size();
	}

	private Output<O> getNextOutput() {
		if(count > 1) {
			return outputs.get((int) (rrc.incrementAndGet() % count));
		} else {
			return outputs.get(0);
		}
	}

	@Override
	public final void put(final O ioTask)
	throws IOException {
		final Output<O> nextOutput = getNextOutput();
		nextOutput.put(ioTask);
	}

	@Override
	public final int put(final List<O> buffer, final int from, final int to)
	throws IOException {
		final Output<O> nextOutput = getNextOutput();
		return nextOutput.put(buffer, from, to);
	}

	@Override
	public final int put(final List<O> buffer)
	throws IOException {
		final Output<O> nextOutput = getNextOutput();
		return nextOutput.put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<O> getInput()
	throws IOException {
		return null;
	}

	@Override
	public final void close()
	throws IOException {
	}
}
