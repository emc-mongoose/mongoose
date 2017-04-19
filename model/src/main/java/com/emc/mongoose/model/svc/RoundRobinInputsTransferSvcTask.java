package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 Created by andrey on 15.12.16.
 */
public final class RoundRobinInputsTransferSvcTask<T>
extends SvcTaskBase {
	
	private final Output<T> output;
	private final List<? extends Input<T>> inputs;
	private final int inputsCount;
	private final AtomicLong rrc = new AtomicLong();

	public RoundRobinInputsTransferSvcTask(
		final Output<T> output, final List<? extends Input<T>> inputs, final List<SvcTask> svcTasks
	) {
		super(svcTasks);
		this.output = output;
		this.inputs = inputs;
		this.inputsCount = inputs.size();
	}

	@Override
	protected final void invoke() {
		final Input<T> nextInput = inputs.get((int) (rrc.getAndIncrement() % inputsCount));
		try {
			final List<T> results = nextInput.getAll();
			LockSupport.parkNanos(1);
			if(results != null) {
				final int resultsCount = results.size();
				if(resultsCount > 0) {
					for(int i = 0; i < resultsCount; i += output.put(results, i, resultsCount))
						;
				}
			}
		} catch(final NoSuchObjectException ignored) {
		} catch(final EOFException e) {
			try {
				close();
			} catch(final IOException ee) {
				ee.printStackTrace(System.err);
			}
		} catch(final RemoteException e) {
			final Throwable cause = e.getCause();
			if(cause instanceof EOFException) {
				try {
					close();
				} catch(final IOException ee) {
					ee.printStackTrace(System.err);
				}
			} else {
				e.printStackTrace(System.err);
			}
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	@Override
	protected final void doClose() {
		inputs.clear();
	}
}
