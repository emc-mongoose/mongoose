package com.emc.mongoose.model.svc;

import com.emc.mongoose.common.collection.OptLockArrayBuffer;
import com.emc.mongoose.common.collection.OptLockBuffer;
import com.emc.mongoose.common.concurrent.SvcTask;
import com.emc.mongoose.common.concurrent.SvcTaskBase;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 Created by andrey on 06.11.16.
 */
public final class RoundRobinOutputsTransferSvcTask<T, O extends Output<T>>
extends SvcTaskBase
implements Output<T> {
	
	private final List<O> outputs;
	private final int outputsCount;
	private final AtomicLong putCounter = new AtomicLong(0);
	private final AtomicLong getCounter = new AtomicLong(0);
	private final int buffCapacity;
	private final Map<O, OptLockBuffer<T>> buffs;

	public RoundRobinOutputsTransferSvcTask(
		final List<O> outputs, final List<SvcTask> svcTasks, final int buffCapacity
	) {
		super(svcTasks);
		this.outputs = outputs;
		this.outputsCount = outputs.size();
		this.buffCapacity = buffCapacity;
		this.buffs = new HashMap<>(this.outputsCount);
		for(int i = 0; i < this.outputsCount; i ++) {
			this.buffs.put(outputs.get(i), new OptLockArrayBuffer<>(buffCapacity));
		}
		svcTasks.add(this);
	}

	private OptLockBuffer<T> selectBuff() {
		if(outputsCount > 1) {
			return buffs.get(outputs.get((int) (putCounter.getAndIncrement() % outputsCount)));
		} else {
			return buffs.get(outputs.get(0));
		}
	}

	@Override
	public final boolean put(final T ioTask)
	throws IOException {
		final OptLockBuffer<T> buff = selectBuff();
		if(buff.tryLock()) {
			try {
				return buff.size() < buffCapacity && buff.add(ioTask);
			} finally {
				buff.unlock();
			}
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<T> srcBuff, final int from, final int to)
	throws IOException {
		OptLockBuffer<T> buff;
		final int n = to - from;
		if(n > outputsCount) {
			final int nPerOutput = n / outputsCount;
			int nextFrom = from;
			for(int i = 0; i < outputsCount; i ++) {
				buff = selectBuff();
				if(buff.tryLock()) {
					try {
						final int m = Math.min(nPerOutput, buffCapacity - buff.size());
						buff.addAll(srcBuff.subList(nextFrom, nextFrom + m));
						nextFrom += m;
					} finally {
						buff.unlock();
					}
				}
			}
			if(nextFrom < to) {
				buff = selectBuff();
				if(buff.tryLock()) {
					try {
						final int m = Math.min(to - nextFrom, buffCapacity - buff.size());
						buff.addAll(srcBuff.subList(nextFrom, nextFrom + m));
						nextFrom += m;
					} finally {
						buff.unlock();
					}
				}
			}
			return nextFrom - from;
		} else {
			for(int i = from; i < to; i ++) {
				buff = selectBuff();
				if(buff.tryLock()) {
					try {
						if(buff.size() < buffCapacity) {
							buff.add(srcBuff.get(i));
						} else {
							return i - from;
						}
					} finally {
						buff.unlock();
					}
				}
			}
			return to - from;
		}
	}

	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}

	@Override
	protected final void invoke() {
		if(outputs.isEmpty()) { // closed already
			return;
		}
		final O output = outputs.get(
			outputsCount > 1 ? (int) (getCounter.getAndIncrement() % outputsCount) : 0
		);
		final OptLockBuffer<T> buff = buffs.get(output);
		if(buff.tryLock()) {
			try {
				int n = buff.size();
				if(n > 0) {
					n = output.put(buff);
					buff.removeRange(0, n);
				}
			} catch(final EOFException ignored) {
			} catch(final RemoteException e) {
				final Throwable cause = e.getCause();
				if(!(cause instanceof EOFException)) {
					e.printStackTrace(System.err);
				}
			} catch(final Throwable t) {
				t.printStackTrace(System.err);
			} finally {
				buff.unlock();
			}
		}
	}

	@Override
	public final Input<T> getInput() {
		throw new AssertionError("Shouldn't be invoked");
	}

	@Override
	protected final void doClose()
	throws IOException {
		for(final O output : outputs) {
			final OptLockBuffer<T> buff = buffs.get(output);
			buff.lock();
			try {
				buff.clear();
			} finally {
				buff.unlock();
			}
		}
		buffs.clear();
		outputs.clear();
	}
}
