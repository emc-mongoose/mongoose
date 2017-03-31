package com.emc.mongoose.common.concurrent;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.io.collection.ListInput;

import java.io.EOFException;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 Created by andrey on 31.03.17.
 */
public final class OutputWrapperSvcTask<T, O extends Output<T>>
extends ArrayList<T>
implements Output<T>, Runnable {

	private final transient O wrappedOutput;
	private final transient Lock lock = new ReentrantLock();
	private final transient int capacity;
	private final transient Set<Runnable> svcTasks;

	public OutputWrapperSvcTask(
		final O wrappedOutput, final int capacity, final Set<Runnable> svcTasks
	) {
		super(capacity);
		this.wrappedOutput = wrappedOutput;
		this.capacity = capacity;
		this.svcTasks = svcTasks;
	}

	@Override
	public final void run() {
		if(lock.tryLock()) {
			try {
				int n = size();
				//System.out.println(hashCode() + ": " + n);
				if(n > 0) {
					n = wrappedOutput.put(this, 0, n);
					System.out.println(hashCode() + ": sent " + n + " I/O tasks to " + wrappedOutput);
					if(n > 0) {
						removeRange(0, n);
					}
				}
			} catch(final EOFException e) {
				svcTasks.remove(this);
			} catch(final RemoteException e) {
				final Throwable cause = e.getCause();
				if(cause instanceof EOFException) {
					svcTasks.remove(this);
				} else {
					e.printStackTrace(System.err);
				}
			} catch(final IOException e) {
				e.printStackTrace(System.err);
			} finally {
				lock.unlock();
			}
		} else {

		}
	}

	@Override
	public final boolean put(final T item)
	throws IOException {
		if(lock.tryLock()) {
			try {
				if(size() < capacity) {
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
				final int n = Math.min(capacity - size(), to - from);
				if(n > 0) {
					addAll(buffer.subList(from, from + n));
					System.out.println(hashCode() + ": put " + n + " I/O tasks to");
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
