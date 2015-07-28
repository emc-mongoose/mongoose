package com.emc.mongoose.common.net.http;
//
import com.emc.mongoose.common.conf.Constants;
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
import java.util.HashMap;
import java.util.Map;
/**
 Created by kurila on 17.03.15.
 */
public final class IOUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static ThreadLocal<Map<Integer, ByteBuffer>>
		THRLOC_BUFF_SIZE_MAP = new ThreadLocal<>();
	/*private final static int
		BUFF_SIZE_STEP = 2, BUFF_SIZES[],
		BUFF_SIZE_MIN, BUFF_SIZE_MAX;
	static {
		int stepCount = 1;
		for(int x = Constants.BUFF_SIZE_LO; x < Constants.BUFF_SIZE_HI; x *= BUFF_SIZE_STEP) {
			stepCount ++;
		}
		BUFF_SIZES = new int[stepCount];
		for(
			int i = 0, nextBuffSize = Constants.BUFF_SIZE_LO;
			nextBuffSize <= Constants.BUFF_SIZE_HI;
			nextBuffSize *= BUFF_SIZE_STEP
		) {
			BUFF_SIZES[i] = nextBuffSize;
			i ++;
		}
		BUFF_SIZE_MIN = BUFF_SIZES[0];
		BUFF_SIZE_MAX = BUFF_SIZES[BUFF_SIZES.length - 1];
	}
	//
	public static long consumeQuietly(final ContentDecoder in, final long expectedSize) {
		//
		int buffSize = 0;
		if(expectedSize < BUFF_SIZE_MIN) {
			buffSize = BUFF_SIZE_MIN;
		} else if(expectedSize > BUFF_SIZE_MAX) {
			buffSize = BUFF_SIZE_MAX;
		} else {
			for(final int nextBuffSize : BUFF_SIZES) {
				if(expectedSize <= nextBuffSize) {
					buffSize = nextBuffSize;
					break;
				}
			}
		}
		//
		long doneByteCount = 0;
		int lastByteCount;
		//
		Map<Integer, ByteBuffer> buffSizeMap = THRLOC_BUFF_SIZE_MAP.get();
		if(buffSizeMap == null) {
			buffSizeMap = new HashMap<>();
			THRLOC_BUFF_SIZE_MAP.set(buffSizeMap);
		}
		//
		ByteBuffer buff = buffSizeMap.get(buffSize);
		if(buff == null) {
			buff = ByteBuffer.allocateDirect(buffSize);
			buffSizeMap.put(buffSize, buff);
		}
		//
		try {
			while(!in.isCompleted()) {
				buff.clear();
				lastByteCount = in.read(buff);
				if(lastByteCount < 0) {
					if(doneByteCount < expectedSize) {
						LOG.warn(
							Markers.MSG, "Expected size {} but got: {}",
							SizeUtil.formatSize(expectedSize), SizeUtil.formatSize(doneByteCount)
						);
					}
					break;
				} else if(lastByteCount > 0) {
					doneByteCount += lastByteCount;
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		//
		if(doneByteCount > expectedSize) {
			LOG.warn(
				Markers.MSG, "Expected size {} but got: {}", SizeUtil.formatSize(expectedSize),
				SizeUtil.formatSize(doneByteCount)
			);
		}
		//
		return doneByteCount;
	}*/
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
		int lastByteCount, nextByteCount = Constants.BUFF_SIZE_LO;
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
						buffSizeMap.hashCode(), buffSizeMap.size(), buffSizeMap.keySet()
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
						if(nextByteCount == Constants.BUFF_SIZE_LO) {
							nextByteCount *= 3;
						} else if(nextByteCount < Constants.BUFF_SIZE_HI) {
							nextByteCount *= 4;
						} // else keep this buffer size
					} else if(lastByteCount < buff.capacity() / 2) { // decrease buffer size
						if(nextByteCount / 3 > Constants.BUFF_SIZE_LO) {
							nextByteCount /= 4;
						} else if(nextByteCount > Constants.BUFF_SIZE_LO) {
							nextByteCount /= 3;
						} // else keep this buffer size
					} // else keep this buffer size
				} else if(lastByteCount < 0) {
					break;
				} else { // decrease buffer size
					if(nextByteCount / 3 > Constants.BUFF_SIZE_LO) {
						nextByteCount /= 4;
					} else if(nextByteCount > Constants.BUFF_SIZE_LO) {
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
	}
	//
}
