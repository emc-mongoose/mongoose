package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 15.12.16.
 */
public final class RoundRobinInputsTransferSvcTask<T>
implements Closeable, Runnable {
	
	private final Output<T> output;
	private final List<? extends Input<T>> inputs;
	private final int inputsCount;
	private final AtomicLong rrc = new AtomicLong();
	private final List<Runnable> svcTasks;

	public RoundRobinInputsTransferSvcTask(
		final Output<T> output, final List<? extends Input<T>> inputs, final List<Runnable> svcTasks
	) {
		this.output = output;
		this.inputs = inputs;
		this.inputsCount = inputs.size();
		this.svcTasks = svcTasks;
	}

	@Override
	public final void run() {
		final Input<T> nextInput = inputs.get((int) (rrc.getAndIncrement() % inputsCount));
		try {
			final List<T> results = nextInput.getAll();
			LockSupport.parkNanos(1);
			if(results != null) {
				final int resultsCount = results.size();
				if(resultsCount > 0) {
					for(int i = 0; i < resultsCount; i += output.put(results, i, resultsCount));
				}
			}
		} catch(final EOFException e) {
			close();
		} catch(final RemoteException e) {
			final Throwable cause = e.getCause();
			if(cause instanceof EOFException) {
				close();
			} else {
				e.printStackTrace(System.err);
			}
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	public final void close() {
		svcTasks.remove(this);
		inputs.clear();
	}
}
