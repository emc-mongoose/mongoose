package com.emc.mongoose.common.io;
//
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.SizeInBytes;
//
import com.emc.mongoose.common.log.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.nio.ByteBuffer;
/**
 Created by kurila on 17.03.15.
 */
public final class IoWorker
extends Thread {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int
		BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(2) + 1);
	//
	private final ByteBuffer[] ioBuffers = new ByteBuffer[BUFF_COUNT];
	//
	private IoWorker(final Runnable task, final String name) {
		super(task, name);
		setDaemon(true);
		//setPriority(Thread.MAX_PRIORITY);
	}
	//
	@Override
	public void run() {
		try {
			super.run();
		} catch(final OutOfMemoryError e) {
			LOG.fatal(
				Markers.ERR, "Not enough free memory. For high load and high performance provide " +
				"more free memory. Use lower concurrency level and/or data sizes otherwise."
			);
		} finally {
			freeBuffers();
		}
	}
	//
	private void freeBuffers() {
		for(int i = 0; i < BUFF_COUNT; i ++) {
			ioBuffers[i] = null;
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			final Runtime rt = Runtime.getRuntime();
			LOG.trace(
				Markers.MSG,
				"Free thread I/O buffers, current memory usage: total={}, max={}, free={}",
				SizeInBytes.formatFixedSize(rt.totalMemory()),
				SizeInBytes.formatFixedSize(rt.maxMemory()),
				SizeInBytes.formatFixedSize(rt.freeMemory())
			);
		}
	}
	//
	public final static class Factory
	extends NamingThreadFactory {
		//
		public Factory(final String threadNamePrefix) {
			super(threadNamePrefix, true);
		}
		//
		@Override
		public IoWorker newThread(final Runnable task) {
			return new IoWorker(task, threadNamePrefix + "#" + threadNumber.incrementAndGet());
		}
	}
	//
	public ByteBuffer getThreadLocalBuff(final long size) {
		int i, currBuffSize = BUFF_SIZE_LO;
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
				if(LOG.isTraceEnabled(Markers.MSG)) {
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
				}
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
					LOG.error(
						Markers.ERR, "Failed to allocate {} of direct memory, " +
							"total direct memory allocated by thread is {}, " +
							"unable to continue using a smaller buffer",
						SizeInBytes.formatFixedSize(currBuffSize),
						SizeInBytes.formatFixedSize(buffSizeSum)
					);
					throw e;
				} else {
					LOG.warn(
						Markers.ERR, "Failed to allocate {} of direct memory, " +
							"total direct memory allocated by thread is {}, " +
							"will continue using smaller buffer of size {}",
						SizeInBytes.formatFixedSize(currBuffSize),
						SizeInBytes.formatFixedSize(buffSizeSum),
						SizeInBytes.formatFixedSize(buff.capacity())
					);
				}
			}
		}
		buff
			.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
