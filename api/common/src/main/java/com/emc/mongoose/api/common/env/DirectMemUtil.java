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
				"Total consumed direct memory: " + SizeInBytes.formatFixedSize(CONSUMED_TOTAL.sum())
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
}
