package com.emc.mongoose.api.model.svc;

import com.emc.mongoose.api.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.api.common.collection.OptLockBuffer;
import com.emc.mongoose.api.common.concurrent.SvcTask;
import com.emc.mongoose.api.common.concurrent.SvcTaskBase;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;
import static com.emc.mongoose.api.common.Constants.KEY_CLASS_NAME;
import static com.emc.mongoose.api.common.Constants.KEY_TEST_STEP_ID;

import org.apache.logging.log4j.CloseableThreadContext;
import static org.apache.logging.log4j.CloseableThreadContext.Instance;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 Created by andrey on 06.05.17.
 The task which tries to transfer the items from the given input to the given output.
 The items got from the input which may not be transferred to the output w/o blocking are stored
 to the deferred tasks buffer.
 */
public class TransferSvcTask<T>
extends SvcTaskBase {

	private final static String CLS_NAME = TransferSvcTask.class.getSimpleName();

	private final String stepName;
	private final Input<T> input;
	private final Output<T> output;
	private final OptLockBuffer<T> deferredItems;

	private int n;

	public TransferSvcTask(
		final List<SvcTask> svcTasks, final String stepName, final Input<T> input,
		final Output<T> output, final int batchSize
	) {
		super(svcTasks);
		this.stepName = stepName;
		this.input = input;
		this.output = output;
		this.deferredItems = new OptLockArrayBuffer<>(batchSize);
	}

	@Override
	protected final void invoke() {
		if(deferredItems.tryLock()) { // works like exclusive invocation lock
			try(
				final Instance ctx = CloseableThreadContext
					.put(KEY_TEST_STEP_ID, stepName)
					.put(KEY_CLASS_NAME, CLS_NAME)
			) {

				// 1st try to output all deferred items if any
				n = deferredItems.size();
				if(n > 0) {
					if(n == 1) {
						if(output.put(deferredItems.get(0))) {
							deferredItems.clear();
						}
					} else {
						n = output.put(deferredItems);
						deferredItems.removeRange(0, n);
					}
					// do not work with new items if there were deferred items
					return;
				}

				final List<T> items = input.getAll();
				if(items != null) {
					n = items.size();
					if(n > 0) {
						if(n == 1) {
							final T item = items.get(0);
							if(!output.put(item)) {
								deferredItems.add(item);
							}
						} else {
							final int m = output.put(items);
							if(m < n) {
								// not all items was transferred w/o blocking
								// defer the remaining items for a future try
								for(final T item : items.subList(m, n)) {
									deferredItems.add(item);
								}
							}
						}
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
			} finally {
				deferredItems.unlock();
			}
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		try {
			deferredItems.tryLock(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
			deferredItems.clear();
		} catch(final InterruptedException e) {
			e.printStackTrace(System.err);
		}
	}
}
