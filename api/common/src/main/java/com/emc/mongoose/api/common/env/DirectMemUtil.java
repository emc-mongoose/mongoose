package com.emc.mongoose.api.common.env;

import com.emc.mongoose.api.common.SizeInBytes;
import sun.nio.ch.DirectBuffer;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.concurrent.atomic.LongAdder;

public interface DirectMemUtil {

	LongAdder CONSUMED_TOTAL = new LongAdder();

	static MappedByteBuffer allocate(final int size)
	throws OutOfMemoryError {
		CONSUMED_TOTAL.add(size);
		try {
			return (MappedByteBuffer) ByteBuffer.allocateDirect(size);
		} catch(final OutOfMemoryError e) {
			System.err.println(
				"Out of direct memory. Total consumed direct memory: " +
					SizeInBytes.formatFixedSize(CONSUMED_TOTAL.sum())
			);
			throw e;
		}
	}

	static boolean deallocate(final MappedByteBuffer buff) {
		if(buff == null) {
			return false;
		}
		((DirectBuffer) buff).cleaner().clean();
		CONSUMED_TOTAL.add(-buff.capacity());
		return true;
	}

	int REUSABLE_BUFF_SIZE_MIN = 1;
	int REUSABLE_BUFF_SIZE_MAX = 0x1000000; // 16MB

	ThreadLocal<MappedByteBuffer[]> REUSABLE_BUFFS = ThreadLocal.withInitial(
		() -> {
			final int buffCount = (int) (
				Math.log(REUSABLE_BUFF_SIZE_MAX / REUSABLE_BUFF_SIZE_MIN) / Math.log(2) + 1
			);
			return new MappedByteBuffer[buffCount];
		}
	);

	static MappedByteBuffer getThreadLocalReusableBuff(final long size)
	throws IllegalArgumentException {

		if(size < 0) {
			throw new IllegalArgumentException("Requested negative buffer size: " + size);
		}

		final MappedByteBuffer[] threadLocalReusableBuffers = REUSABLE_BUFFS.get();
		long currBuffSize = Long.highestOneBit(size);
		if(currBuffSize > REUSABLE_BUFF_SIZE_MAX) {
			currBuffSize = REUSABLE_BUFF_SIZE_MAX;
		} else if(currBuffSize < REUSABLE_BUFF_SIZE_MAX) {
			if(currBuffSize < REUSABLE_BUFF_SIZE_MIN) {
				currBuffSize = REUSABLE_BUFF_SIZE_MIN;
			} else if(currBuffSize < size) {
				currBuffSize <<= 1;
			}
		}
		final int i = Long.numberOfTrailingZeros(currBuffSize);
		MappedByteBuffer buff = threadLocalReusableBuffers[i];

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
			threadLocalReusableBuffers[i] = buff;
		}

		buff
			.position(0)
			.limit(size < buff.capacity() ? Math.max(1, (int) size) : buff.capacity());
		return buff;
	}
}
