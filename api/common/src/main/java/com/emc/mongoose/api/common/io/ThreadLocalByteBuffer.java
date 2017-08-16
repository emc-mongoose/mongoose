package com.emc.mongoose.api.common.io;

import com.emc.mongoose.api.common.env.DirectMemUtil;

import static com.emc.mongoose.api.common.SizeInBytes.formatFixedSize;

import java.nio.MappedByteBuffer;

/**
 Created by kurila on 13.10.16.
 */
public final class ThreadLocalByteBuffer {
	
	public static final int SIZE_MIN = 1;
	public static final int SIZE_MAX = 0x1000000; // 16MB
	
	private static final ThreadLocal<MappedByteBuffer[]> BUFFERS = ThreadLocal.withInitial(
		() -> {
			final int buffCount = (int) (
				Math.log(SIZE_MAX / SIZE_MIN) / Math.log(2) + 1
			);
			return new MappedByteBuffer[buffCount];
		}
	);
	
	public static MappedByteBuffer get(final long size)
	throws IllegalArgumentException {
		
		if(size < 0) {
			throw new IllegalArgumentException("Requested negative buffer size: " + size);
		}
		
		final MappedByteBuffer[] ioBuffers = BUFFERS.get();
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
		MappedByteBuffer buff = ioBuffers[i];
		
		if(buff == null) {
			buff = DirectMemUtil.allocate((int) currBuffSize);
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
