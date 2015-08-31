package com.emc.mongoose.common.net.http;
//
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
//
import com.emc.mongoose.common.concurrent.GroupThreadFactory;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
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
public final class IOUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int
		//BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(4) + 1);
		BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(2));
	//
	private static class IOWorker
	extends Thread {
		//
		private final ByteBuffer[] ioBuffSeq = new ByteBuffer[BUFF_COUNT];
		private final RunTimeConfig rtConfig;
		//
		protected IOWorker(final Runnable task, final String name, final RunTimeConfig rtConfig) {
			super(task, name);
			this.rtConfig = rtConfig;
		}
		//
		@Override
		public void run() {
			RunTimeConfig.setContext(rtConfig); // for integ tests
			try {
				super.run();
			} finally {
				releaseMem();
			}
		}
		//
		private void releaseMem() {
			ByteBuffer bbuff;
			for(int i = 0; i < ioBuffSeq.length; i++) {
				bbuff = ioBuffSeq[i];
				if(bbuff != null) {
					bbuff.clear();
					LOG.debug(
						Markers.MSG, "{}: release {} bytes of direct memory",
						getName(), bbuff.capacity()
					);
					ioBuffSeq[i] = null;
				}
			}
			bbuff = null;
		}
	}
	//
	public final static class IOWorkerFactory
	extends GroupThreadFactory {
		//
		private final RunTimeConfig rtConfig;
		//
		public IOWorkerFactory(final String threadNamePrefix, final RunTimeConfig rtConfig) {
			super(threadNamePrefix);
			this.rtConfig = rtConfig;
		}
		//
		@Override
		public Thread newThread(final Runnable task) {
			return new IOWorker(task, getName() + "#" + threadNumber.incrementAndGet(), rtConfig);
		}
	}
	/*
	public static long consumeQuietlyBIO(final ContentDecoder in)
	throws IllegalStateException {
		//
		final Thread currThread = Thread.currentThread();
		if(!IOWorker.class.isInstance(currThread)) {
			throw new IllegalStateException();
		}
		final ByteBuffer ioBuffSeq[] = ((IOWorker) currThread).ioBuffSeq;
		//
		int i = 0, nextBuffSize = BUFF_SIZE_LO, doneSize;
		long doneSizeSum = 0;
		ByteBuffer buff;
		//
		try {
			while(!in.isCompleted()) {
				// obtain the buffer
				buff = ioBuffSeq[i];
				if(buff == null) {
					buff = ByteBuffer.allocateDirect(nextBuffSize);
					LOG.debug(Markers.MSG, "allocated {} bytes of direct memory", nextBuffSize);
					ioBuffSeq[i] = buff;
					if(LOG.isTraceEnabled(Markers.MSG)) {
						final StringBuilder sb = new StringBuilder(Thread.currentThread().getName())
							.append(": ");
						for(final ByteBuffer bb : ioBuffSeq) {
							if(bb != null) {
								sb.append(SizeUtil.formatSize(bb.capacity())).append(", ");
							}
						}
						LOG.trace(Markers.MSG, sb.toString());
					}
				} else {
					buff.clear();
				}
				// read
				doneSize = in.read(buff);
				// analyze
				if(doneSize < 0) {
					break;
				} else if(i > 0 && doneSize < nextBuffSize / 4) {
					// doneSize < 0.25 * nextSize -> decrease buff size
					i --;
					if(i == 0) {
						nextBuffSize /= 3;
					} else {
						nextBuffSize /= 4;
					}
				} else if(i < BUFF_COUNT - 1 && 4 * doneSize > 3 * nextBuffSize) {
					// doneSize > 0.75 * nextSize -> increase buff size
					i ++;
					if(i == 1) {
						nextBuffSize *= 3;
					} else {
						nextBuffSize *= 4;
					}
				} // else keep buff size the same
				// increment the read bytes count
				doneSizeSum += doneSize;
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		//
		return doneSizeSum;
	}*/
	//
	public static int consumeQuietlyNIO(final ContentDecoder in, final long expectedByteCount) {
		//
		final Thread currThread = Thread.currentThread();
		if(!IOWorker.class.isInstance(currThread)) {
			throw new IllegalStateException();
		}
		final ByteBuffer ioBuffSeq[] = ((IOWorker) currThread).ioBuffSeq;
		//
		int doneByteCount = 0;
		try {
			if(!in.isCompleted()) {
				//
				int i, currBuffSize = BUFF_SIZE_LO;
				for(i = 0; i < ioBuffSeq.length && currBuffSize < expectedByteCount; i ++) {
					currBuffSize *= 2;
				}
				//
				ByteBuffer buff = ioBuffSeq[i];
				if(buff == null) {
					buff = ByteBuffer.allocateDirect(currBuffSize);
					ioBuffSeq[i] = buff;
				} else {
					buff.clear();
				}
				//
				doneByteCount = in.read(buff);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		return doneByteCount;
	}
}
