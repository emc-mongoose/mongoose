package com.emc.mongoose.tests.unit;

import com.emc.mongoose.api.common.io.ThreadLocalByteBuffer;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.*;

/**
 Created by kurila on 12.01.17.
 */
public final class ThreadLocalByteBufferTest {
	
	@Test
	public final void testZeroBytesRequest()
	throws Exception {
		final ByteBuffer bb = ThreadLocalByteBuffer.get(0);
		assertEquals(bb.capacity(), ThreadLocalByteBuffer.SIZE_MIN);
	}
	
	@Test
	public final void testOneByteRequest()
	throws Exception {
		final ByteBuffer bb = ThreadLocalByteBuffer.get(1);
		assertEquals(bb.capacity(), 1);
	}
	
	@Test
	public final void testNegativeBytesRequestThrowsException()
	throws Exception {
		try {
			final ByteBuffer bb = ThreadLocalByteBuffer.get(-100000);
			fail("Exception was not thrown as expected");
		} catch(final IllegalArgumentException e) {
			assertNotNull(e);
		}
	}
	
	@Test
	public final void testSomeBytesRequest1()
	throws Exception {
		final ByteBuffer bb = ThreadLocalByteBuffer.get(12345);
		assertEquals(bb.capacity(), 0x4000);
	}
	
	@Test
	public final void testSomeBytesRequest2()
	throws Exception {
		final ByteBuffer bb = ThreadLocalByteBuffer.get(0x40000);
		assertEquals(bb.capacity(), 0x40000);
	}
	
	@Test
	public final void testTooManyBytesRequest() {
		final ByteBuffer bb = ThreadLocalByteBuffer.get(1_000_000_000_000L);
		assertEquals(bb.capacity(), ThreadLocalByteBuffer.SIZE_MAX);
	}
}
