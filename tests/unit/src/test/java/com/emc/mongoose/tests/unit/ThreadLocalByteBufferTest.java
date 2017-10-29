package com.emc.mongoose.tests.unit;

import com.github.akurilov.commons.system.DirectMemUtil;

import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 Created by kurila on 12.01.17.
 */
public final class ThreadLocalByteBufferTest {
	
	@Test
	public final void testZeroBytesRequest()
	throws Exception {
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(0);
		assertEquals(bb.capacity(), DirectMemUtil.REUSABLE_BUFF_SIZE_MIN);
	}
	
	@Test
	public final void testOneByteRequest()
	throws Exception {
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(1);
		assertEquals(bb.capacity(), 1);
	}
	
	@Test
	public final void testNegativeBytesRequestThrowsException()
	throws Exception {
		try {
			final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(-100000);
			fail("Exception was not thrown as expected");
		} catch(final IllegalArgumentException e) {
			assertNotNull(e);
		}
	}
	
	@Test
	public final void testSomeBytesRequest1()
	throws Exception {
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(12345);
		assertEquals(bb.capacity(), 0x4000);
	}
	
	@Test
	public final void testSomeBytesRequest2()
	throws Exception {
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(0x40000);
		assertEquals(bb.capacity(), 0x40000);
	}
	
	@Test
	public final void testTooManyBytesRequest() {
		final ByteBuffer bb = DirectMemUtil.getThreadLocalReusableBuff(1_000_000_000_000L);
		assertEquals(bb.capacity(), DirectMemUtil.REUSABLE_BUFF_SIZE_MAX);
	}
}
