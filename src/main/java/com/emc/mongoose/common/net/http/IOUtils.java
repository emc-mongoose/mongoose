package com.emc.mongoose.common.net.http;
//
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
//
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
//import java.util.HashMap;
//import java.util.Map;
/**
 Created by kurila on 17.03.15.
 */
public final class IOUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	/*
	private final static ThreadLocal<Map<Integer, ByteBuffer>>
		THRLOC_BUFF_SIZE_MAP = new ThreadLocal<>();
	//
	public static long consumeQuietly(final ContentDecoder in) {
		long doneByteCount = 0;
		//
		Map<Integer, ByteBuffer> buffSizeMap = THRLOC_BUFF_SIZE_MAP.get();
		if(buffSizeMap == null) {
			buffSizeMap = new HashMap<>();
			THRLOC_BUFF_SIZE_MAP.set(buffSizeMap);
		}
		//
		int lastByteCount, nextByteCount = BUFF_SIZE_LO;
		ByteBuffer buff;
		//
		try {
			while(!in.isCompleted()) {
				//
				buff = buffSizeMap.get(nextByteCount);
				if(buff == null) {
					buff = ByteBuffer.allocateDirect(nextByteCount);
					buffSizeMap.put(nextByteCount, buff);
					LOG.debug(
						Markers.MSG,
						"Thread local direct memory buffer map changed: count: {}, sizes: {}",
						buffSizeMap.size(), buffSizeMap.keySet()
					);
				} else {
					buff.clear();
				}
				//
				lastByteCount = in.read(buff);
				// try to adapt the direct buffer size
				if(lastByteCount > 0) {
					doneByteCount += lastByteCount;
					if(lastByteCount >= buff.capacity()) { // increase buffer size
						if(nextByteCount == BUFF_SIZE_LO) {
							nextByteCount *= 3;
						} else if(nextByteCount < BUFF_SIZE_HI) {
							nextByteCount *= 4;
						} // else keep this buffer size
					} else if(lastByteCount < buff.capacity() / 2) { // decrease buffer size
						if(nextByteCount / 3 > BUFF_SIZE_LO) {
							nextByteCount /= 4;
						} else if(nextByteCount > BUFF_SIZE_LO) {
							nextByteCount /= 3;
						} // else keep this buffer size
					} // else keep this buffer size
				} else if(lastByteCount < 0) {
					break;
				} else { // decrease buffer size
					if(nextByteCount / 3 > BUFF_SIZE_LO) {
						nextByteCount /= 4;
					} else if(nextByteCount > BUFF_SIZE_LO) {
						nextByteCount /= 3;
					} // else keep this buffer size
				}
				//
				LOG.trace(
					Markers.MSG, "Byte count: done {}, last {}, next {}",
					doneByteCount, lastByteCount, nextByteCount
				);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		//
		return doneByteCount;
	}*/
	//
	private final static ThreadLocal<ByteBuffer[]> THRLOC_BUFF_SEQ = new ThreadLocal<>();
	private final static int
		BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(4) + 1);
	//
	public static long consumeQuietly(final ContentDecoder in) {
		//
		ByteBuffer[] buffs = THRLOC_BUFF_SEQ.get();
		if(buffs == null) {
			buffs = new ByteBuffer[BUFF_COUNT];
			THRLOC_BUFF_SEQ.set(buffs);
		}
		//
		int i = 0, nextSize = BUFF_SIZE_LO, doneSize;
		long doneSizeSum = 0;
		ByteBuffer buff;
		//
		try {
			while(!in.isCompleted()) {
				// obtain the buffer
				buff = buffs[i];
				if(buff == null) {
					buff = ByteBuffer.allocateDirect(nextSize);
					buffs[i] = buff;
					if(LOG.isTraceEnabled(Markers.MSG)) {
						final StringBuilder sb = new StringBuilder(Thread.currentThread().getName())
							.append(": ");
						for(final ByteBuffer bb : buffs) {
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
				} else if(i > 0 && doneSize < nextSize / 4) {
					// doneSize < 0.25 * nextSize -> decrease buff size
					i --;
					if(i == 0) {
						nextSize /= 3;
					} else {
						nextSize /= 4;
					}
				} else if(i < BUFF_COUNT - 1 && 4 * doneSize > 3 * nextSize) {
					// doneSize > 0.75 * nextSize -> increase buff size
					i ++;
					if(i == 1) {
						nextSize *= 3;
					} else {
						nextSize *= 4;
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
	}
	//
	public static void releaseUsedDirectMemory() {
		final ByteBuffer buffSeq[] = THRLOC_BUFF_SEQ.get();
		if(buffSeq != null) {
			for(int i = 0; i < buffSeq.length; i ++) {
				if(buffSeq[i] != null) {
					buffSeq[i].clear();
					buffSeq[i] = null;
				}
			}
		}
	}
}
