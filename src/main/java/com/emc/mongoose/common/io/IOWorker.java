package com.emc.mongoose.common.io;
//
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import org.apache.http.nio.ContentDecoder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
/**
 Created by kurila on 17.03.15.
 */
public final class IOWorker
extends Thread {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int
		BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(2) + 1);
	//
	private final ByteBuffer[] ioBuffers = new ByteBuffer[BUFF_COUNT];
	//
	private IOWorker(final Runnable task, final String name) {
		super(task, name);
	}
	//
	@Override
	protected final void finalize()
	throws Throwable {
		for(int i = 0; i < BUFF_COUNT; i ++) {
			ioBuffers[i] = null;
		}
		super.finalize();
	}
	//
	public final static class Factory
	extends GroupThreadFactory {
		//
		public Factory(final String threadNamePrefix) {
			super(threadNamePrefix);
		}
		//
		@Override
		public IOWorker newThread(final Runnable task) {
			return new IOWorker(task, getName() + "#" + threadNumber.incrementAndGet());
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
						SizeUtil.formatSize(currBuffSize), SizeUtil.formatSize(buffSizeSum)
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
						SizeUtil.formatSize(currBuffSize), SizeUtil.formatSize(buffSizeSum)
					);
					throw e;
				} else {
					LOG.warn(
						Markers.ERR, "Failed to allocate {} of direct memory, " +
							"total direct memory allocated by thread is {}, " +
							"will continue using smaller buffer of size {}",
						SizeUtil.formatSize(currBuffSize), SizeUtil.formatSize(buffSizeSum),
						SizeUtil.formatSize(buff.capacity())
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
