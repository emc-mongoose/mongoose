package com.emc.mongoose.common.io;

import static com.emc.mongoose.common.api.SizeInBytes.formatFixedSize;

import java.nio.ByteBuffer;

/**
 Created by kurila on 13.10.16.
 */
public final class ThreadLocalByteBuffer {
	
	public static final int SIZE_MIN = 1;
	public static final int SIZE_MAX = 0x100000; // 1MB
	
	private static final ThreadLocal<ByteBuffer[]> BUFFERS = new ThreadLocal<ByteBuffer[]>() {
		@Override
		protected final ByteBuffer[] initialValue() {
			final int buffCount = (int) (
				Math.log(SIZE_MAX / SIZE_MIN) / Math.log(2) + 1
			);
			return new ByteBuffer[buffCount];
		}
	};
	
	public static ByteBuffer get(final long size) {
		
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
			buff = ByteBuffer.allocateDirect(currBuffSize);
			long buffSizeSum = 0;
			for(final ByteBuffer ioBuff : ioBuffers) {
				if(ioBuff != null) {
					buffSizeSum += ioBuff.capacity();
				}
			}
			System.out.println(
				"Allocated " + formatFixedSize(currBuffSize) +
				" of direct memory, total used by the thread: " + formatFixedSize(buffSizeSum)
			);
			ioBuffers[i] = buff;
		}
		
		buff
			.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
