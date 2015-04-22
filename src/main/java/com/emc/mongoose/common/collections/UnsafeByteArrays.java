package com.emc.mongoose.common.collections;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.lang.reflect.Field;
import java.nio.ByteOrder;
//
import sun.misc.Unsafe;
/**
 Created by kurila on 22.04.15.
 Used:
 http://cr.openjdk.java.net/~psandoz/unsafe/ByteArrayCompareTest.java
 */
public final class UnsafeByteArrays {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static Unsafe UNSAFE;
	private final static int LONG_BYTES = Long.SIZE / Byte.SIZE;
	private final static boolean BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
	private final static int UNSIGNED_MASK = 0xFF;
	private final static int BYTE_ARRAY_BASE_OFFSET;
	//
	static {
		try {
			Field uf = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
			uf.setAccessible(true);
			UNSAFE = sun.misc.Unsafe.class.cast(uf.get(null));
			//
			BYTE_ARRAY_BASE_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.ERROR, e, "Failure");
			throw new RuntimeException(e);
		}
	}
	//
	public static boolean equals(byte[] left, byte[] right) {
		final int minLength = Math.min(left.length, right.length);
		final int minWords = minLength / LONG_BYTES;
		long lw, rw;
		for (int i = 0; i < minWords * LONG_BYTES; i += LONG_BYTES) {
			lw = UNSAFE.getLong(left, BYTE_ARRAY_BASE_OFFSET + (long) i);
			rw = UNSAFE.getLong(right, BYTE_ARRAY_BASE_OFFSET + (long) i);
			if(lw != rw) {
				if(BIG_ENDIAN) {
					return lw == -rw;
				}
				int n = Long.numberOfTrailingZeros(lw ^ rw) & ~0x7;
				return 0 == (((lw >>> n) & UNSIGNED_MASK) - ((rw >>> n) & UNSIGNED_MASK));
			}
		}
		//
		for (int i = minWords * LONG_BYTES; i < minLength; i++) {
			if(left[i] != right[i]) {
				return false;
			}
		}
		return left.length == right.length;
	}
}
