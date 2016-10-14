package com.emc.mongoose.common.io;

import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;

import java.nio.ByteBuffer;

/**
 Created by kurila on 13.10.16.
 */
public final class ThreadLocalByteBuffer {
	
	public final static int SIZE_MIN = 1;
	public final static int SIZE_MAX = 0x100000; // 1MB
	
	private final static ThreadLocal<ByteBuffer[]> BUFFERS = new ThreadLocal<ByteBuffer[]>() {
		@Override
		protected final ByteBuffer[] initialValue() {
			final int buffCount = (int) (
				Math.log(SIZE_MAX / SIZE_MIN) / Math.log(2) + 1
			);
			return new ByteBuffer[buffCount];
		}
	};
	
	public final static ByteBuffer get(final long size) {
		
		final ByteBuffer[] ioBuffers = BUFFERS.get();
		int i, currBuffSize = SIZE_MIN;
		for(i = 0; i < ioBuffers.length && currBuffSize < size; i ++) {
			currBuffSize *= 2;
		}
		
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
		
		buff
			.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
