package com.emc.mongoose.model.item;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.collection.ListInput;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.START_OFFSET_MICROS;

import java.io.EOFException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;
import static java.lang.System.nanoTime;

/**
 Created by kurila on 16.01.17.
 */
public final class BasicIoResultsItemInput<I extends Item, O extends IoTask<I>>
implements IoResultsItemInput<I, O> {
	
	private final List<O> ioResultsBuff;
	private volatile int ioResultsBuffSize = 0;
	private final int ioResultsBuffCapacity;
	private final long delayMicroseconds;

	private volatile boolean poisonedFlag = false;

	public BasicIoResultsItemInput(
		final int queueCapacity, final TimeUnit timeUnit, final long delay
	) {
		this.ioResultsBuff = new LinkedList<>();
		this.ioResultsBuffCapacity = queueCapacity;
		this.delayMicroseconds = timeUnit.toMicros(delay);
	}

	@Override
	public final synchronized boolean put(final O ioResult)
	throws IOException {
		if(ioResult == null) {
			return poisonedFlag = true;
		}
		if(ioResultsBuffSize < ioResultsBuffCapacity) {
			ioResultsBuff.add(ioResult);
			ioResultsBuffSize ++;
			return true;
		} else {
			return false;
		}
	}
	
	@Override
	public final synchronized int put(final List<O> ioResults, final int from, final int to)
	throws IOException {
		final int n = Math.min(ioResultsBuffCapacity - ioResultsBuffSize, to - from);
		O ioResult;
		for(int i = 0; i < n; i ++) {
			ioResult = ioResults.get(i + from);
			if(ioResult == null) {
				poisonedFlag = true;
				ioResultsBuffSize += i;
				return i;
			}
			ioResultsBuff.add(ioResult);
		}
		ioResultsBuffSize += n;
		return n;
	}
	
	@Override
	public final synchronized int put(final List<O> ioResults)
	throws IOException {
		final int n = Math.min(ioResultsBuffCapacity - ioResultsBuffSize, ioResults.size());
		O ioResult;
		for(int i = 0; i < n; i ++) {
			ioResult = ioResults.get(i);
			if(ioResult == null) {
				poisonedFlag = true;
				ioResultsBuffSize += i;
				return i;
			}
			ioResultsBuff.add(ioResult);
		}
		ioResultsBuffSize += n;
		return n;
	}

	/** Please don't use this method, it's unsafe and provided just for convenience */
	@Override
	public final Input<O> getInput()
	throws IOException {
		throw new AssertionError();
	}
	
	@Override
	public final synchronized I get()
	throws EOFException, IOException {

		if(ioResultsBuffSize == 0 && poisonedFlag) {
			throw new EOFException();
		}

		O nextIoResult;
		long nextFinishTime, currTime;
		I item = null;
		final ListIterator<O> ioResultsIter = ioResultsBuff.listIterator();
		while(ioResultsIter.hasNext()) {
			nextIoResult = ioResultsIter.next();
			if(delayMicroseconds > 0) {
				nextFinishTime = nextIoResult.getRespTimeDone();
				currTime = START_OFFSET_MICROS + nanoTime() / 1000;
				if(currTime - nextFinishTime > delayMicroseconds) {
					item = nextIoResult.getItem();
					ioResultsIter.remove();
					ioResultsBuffSize --;
					break;
				}
			} else {
				item = nextIoResult.getItem();
				ioResultsIter.remove();
				ioResultsBuffSize --;
				break;
			}
		}
		return item;
	}
	
	@Override
	public final synchronized int get(final List<I> buffer, final int limit)
	throws IOException {

		if(ioResultsBuffSize == 0 && poisonedFlag) {
			throw new EOFException();
		}

		O nextIoResult;
		long nextFinishTime, currTime;
		int n = 0;
		final ListIterator<O> ioResultsIter = ioResultsBuff.listIterator();
		if(delayMicroseconds > 0) {
			while(ioResultsIter.hasNext()) {
				nextIoResult = ioResultsIter.next();
				nextFinishTime = nextIoResult.getRespTimeDone();
				currTime = START_OFFSET_MICROS + nanoTime() / 1000;
				if(currTime - nextFinishTime > delayMicroseconds) {
					buffer.add(nextIoResult.getItem());
					ioResultsIter.remove();
					ioResultsBuffSize --;
					n ++;
				}
			}
		} else {
			while(ioResultsIter.hasNext()) {
				buffer.add(ioResultsIter.next().getItem());
				ioResultsIter.remove();
				ioResultsBuffSize --;
				n ++;
			}
		}
		return n;
	}
	
	@Override
	public final synchronized long skip(final long count)
	throws IOException {
		final ListIterator<O> ioResultsIter = ioResultsBuff.listIterator();
		long n;
		for(n = 0; n < count; n ++) {
			if(ioResultsIter.hasNext()) {
				ioResultsIter.remove();
			} else {
				break;
			}
		}
		return n;
	}
	
	@Override
	public final void reset()
	throws IOException {
	}
	
	@Override
	public final synchronized void close()
	throws IOException {
		ioResultsBuff.clear();
		ioResultsBuffSize = 0;
	}

	@Override
	public final String toString() {
		if(delayMicroseconds > 0) {
			return "PreviousItemsWithDelay" + (delayMicroseconds / 1_000_000) + "s";
		} else {
			return "PreviousItems";
		}
	}
}
