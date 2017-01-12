package com.emc.mongoose.common.io;

import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;

import java.nio.ByteBuffer;

/**
 Created by kurila on 13.10.16.
 */
public final class ThreadLocalByteBuffer {
	
	public static final int SIZE_MIN = 1;
	public static final int SIZE_MAX = 0x1000000; // 16MB
	
	private static final ThreadLocal<ByteBuffer[]> BUFFERS = new ThreadLocal<ByteBuffer[]>() {
		@Override
		protected final ByteBuffer[] initialValue() {
			final int buffCount = (int) (
				Math.log(SIZE_MAX / SIZE_MIN) / Math.log(2) + 1
			);
			return new ByteBuffer[buffCount];
		}
	};
	
	public static ByteBuffer get(final long size)
	throws IllegalArgumentException {
		
		if(size < 0) {
			throw new IllegalArgumentException("Requested negative buffer size: " + size);
		}
		
		final ByteBuffer[] ioBuffers = BUFFERS.get();
		long currBuffSize = Long.highestOneBit(size);
		if(currBuffSize > SIZE_MAX) {
			currBuffSize = SIZE_MAX;
		} else if(currBuffSize < SIZE_MAX) {
			if(currBuffSize < SIZE_MIN) {
				currBuffSize = SIZE_MIN;
			} else if(currBuffSize < size) {
				currBuffSize <<= 1;
			}
		}
		final int i = Long.numberOfTrailingZeros(currBuffSize);
		ByteBuffer buff = ioBuffers[i];
		
		if(buff == null) {
			buff = ByteBuffer.allocateDirect((int) currBuffSize);
			/*long buffSizeSum = 0;
			for(final ByteBuffer ioBuff : ioBuffers) {
				if(ioBuff != null) {
					buffSizeSum += ioBuff.capacity();
				}
			}
			System.out.println(
				Thread.currentThread().getName() + ": allocated " + formatFixedSize(currBuffSize) +
				" of direct memory, total used by the thread: " + formatFixedSize(buffSizeSum)
			);*/
			ioBuffers[i] = buff;
		}
		
		buff
			.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
