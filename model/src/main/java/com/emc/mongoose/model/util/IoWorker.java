package com.emc.mongoose.model.util;

import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import java.nio.ByteBuffer;
import static com.emc.mongoose.model.util.SizeInBytes.formatFixedSize;

/**
 Created by kurila on 17.03.15.
 */
public final class IoWorker
extends Thread {
	//
	private final int minBuffSize;
	private final int buffCount;
	private final ByteBuffer[] ioBuffers;
	//
	private IoWorker(
		final Runnable task, final String name, final int minBuffSize, final int buffSizeMax
	) {
		super(task, name);
		setDaemon(true);
		//setPriority(Thread.MAX_PRIORITY);
		this.minBuffSize = minBuffSize;
		this.buffCount = (int) (Math.log(buffSizeMax / minBuffSize) / Math.log(2) + 1);
		ioBuffers = new ByteBuffer[buffCount];
	}
	//
	@Override
	public void run() {
		try {
			super.run();
		} catch(final OutOfMemoryError e) {
			System.err.println(
				"Not enough free memory. For high load and high performance provide " +
				"more free memory. Use lower concurrency level and/or data sizes otherwise."
			);
			e.printStackTrace(System.err);
		} finally {
			freeBuffers();
		}
	}
	//
	private void freeBuffers() {
		for(int i = 0; i < buffCount; i ++) {
			ioBuffers[i] = null;
		}
		/*if(LOG.isTraceEnabled(Markers.MSG)) {
			final Runtime rt = Runtime.getRuntime();
			LOG.trace(
				Markers.MSG,
				"Free thread I/O buffers, current memory usage: total={}, max={}, free={}",
				SizeInBytes.formatFixedSize(rt.totalMemory()),
				SizeInBytes.formatFixedSize(rt.maxMemory()),
				SizeInBytes.formatFixedSize(rt.freeMemory())
			);
		}*/
	}
	//
	public final static class Factory
	extends NamingThreadFactory {
		//
		private final int minBuffSize;
		private final int maxBuffSize;
		//
		public Factory(
			final String threadNamePrefix, final int minBuffSize, final int maxBuffSize
		) {
			super(threadNamePrefix, true);
			this.minBuffSize = minBuffSize;
			this.maxBuffSize = maxBuffSize;
		}
		//
		@Override
		public IoWorker newThread(final Runnable task) {
			return new IoWorker(
				task, threadNamePrefix + "#" + threadNumber.incrementAndGet(), minBuffSize,
				maxBuffSize
			);
		}
	}
	//
	public ByteBuffer getThreadLocalBuff(final long size) {
		int i, currBuffSize = minBuffSize;
		for(i = 0; i < ioBuffers.length && currBuffSize < size; i ++) {
			currBuffSize *= 2;
		}
		//
		if(i == ioBuffers.length) {
			i --;
		}
		ByteBuffer buff = ioBuffers[i];
		if(buff == null) {
			try {
				buff = ByteBuffer.allocateDirect(currBuffSize);
				/*if(LOG.isTraceEnabled(Markers.MSG)) {
					long buffSizeSum = 0;
					for(final ByteBuffer ioBuff : ioBuffers) {
						if(ioBuff != null) {
							buffSizeSum += ioBuff.capacity();
						}
					}
					LOG.trace(
						Markers.MSG, "Allocated {} of direct memory, total used by the thread: {}",
						SizeInBytes.formatFixedSize(currBuffSize),
						SizeInBytes.formatFixedSize(buffSizeSum)
					);
				}*/
				ioBuffers[i] = buff;
			} catch(final OutOfMemoryError e) {
				long buffSizeSum = 0;
				for(final ByteBuffer smallerBuff : ioBuffers) {
					if(smallerBuff != null) {
						buffSizeSum += smallerBuff.capacity();
						if(currBuffSize > smallerBuff.capacity()) {
							buff = smallerBuff;
						}
					}
				}
				if(buff == null) {
					/*LOG.error(
						Markers.ERR, "Failed to allocate {} of direct memory, " +
							"total direct memory allocated by thread is {}, " +
							"unable to continue using a smaller buffer",
						SizeInBytes.formatFixedSize(currBuffSize),
						SizeInBytes.formatFixedSize(buffSizeSum)
					);*/
					throw e;
				} else {
					System.err.println(
						"Failed to allocate " + formatFixedSize(currBuffSize) +
						" of direct memory, total direct memory allocated by thread is " +
						formatFixedSize(buffSizeSum) + ", will continue using smaller buffer of " +
						"size " + formatFixedSize(buff.capacity())
					);
				}
			}
		}
		buff.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
