package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.ListInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by andrey on 31.03.17.
 */
public final class OutputWrapperSvcTask<T, O extends Output<T>>
extends ArrayList<T>
implements Output<T>, Runnable {

	private final O wrappedOutput;
	private final Lock lock = new ReentrantLock();
	private final int capaicity;
	private int n;

	public OutputWrapperSvcTask(final O wrappedOutput, final int capacity) {
		super(capacity);
		this.wrappedOutput = wrappedOutput;
		this.capaicity = capacity;
	}

	@Override
	public final void run() {
		if(lock.tryLock()) {
			try {
				n = wrappedOutput.put(this, 0, size());
				if(n > 0) {
					removeRange(0, n);
				} else {
					LockSupport.parkNanos(1);
				}
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			} finally {
				lock.unlock();
			}
		} else {
			LockSupport.parkNanos(1);
		}
	}

	@Override
	public final boolean put(final T item)
	throws IOException {
		if(lock.tryLock()) {
			try {
				if(size() < capaicity) {
					return add(item);
				} else {
					return false;
				}
			} finally {
				lock.unlock();
			}
		} else {
			return false;
		}
	}

	@Override
	public final int put(final List<T> buffer, final int from, final int to)
	throws IOException {
		if(lock.tryLock()) {
			try {
				final int n = Math.min(capaicity - size(), to - from);
				if(n > 0) {
					addAll(buffer.subList(from, from + n));
				}
				return n;
			} finally {
				lock.unlock();
			}
		} else {
			return 0;
		}
	}

	@Override
	public final int put(final List<T> buffer)
	throws IOException {
		return put(buffer, 0, buffer.size());
	}

	@Override
	public final Input<T> getInput()
	throws IOException {
		return new ListInput<>(this);
	}

	/**
	 Please note that this method doesn't close the wrapped output
	 @throws IOException
	 */
	@Override
	public final void close() {
		lock.lock();
		clear();
		lock.unlock();
	}
}
