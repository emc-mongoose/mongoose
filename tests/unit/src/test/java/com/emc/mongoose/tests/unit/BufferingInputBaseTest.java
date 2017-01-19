package com.emc.mongoose.common.io.collection;

import com.emc.mongoose.common.io.Input;
import org.junit.Test;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 Created by andrey on 02.12.16.
 */
public class BufferingInputBaseTest {

	@Test
	public final void test0()
	throws Exception {
		final int totalCount = 1_234_567;
		final AtomicInteger doneCount = new AtomicInteger(0);
		final Input<Integer> in = new BufferingInputBase<Integer>(1_000) {
			@Override
			protected int loadMoreItems(final Integer last) {
				int n;
				for(n = 0; n < capacity && doneCount.get() < totalCount; n++) {
					items.add(doneCount.getAndIncrement());
				}
				return n;
			}
		};
		final List<Integer> buff = new ArrayList<>(0x100);
		int m = 0, k;
		while(true) {
			try {
				k = in.get(buff, 0x100);
			} catch(final EOFException e) {
				break;
			}
			assertEquals(k, buff.size());
			buff.clear();
			m += k;
		}
		assertEquals(totalCount, m);
		assertEquals(m, doneCount.get());
	}

	@Test
	public final void test1()
	throws Exception {
		final int totalCount = 10;
		final AtomicInteger doneCount = new AtomicInteger(0);
		final Input<Integer> in = new BufferingInputBase<Integer>(1_000) {
			@Override
			protected int loadMoreItems(final Integer last) {
				int n;
				for(n = 0; n < capacity && doneCount.get() < totalCount; n++) {
					items.add(doneCount.getAndIncrement());
				}
				return n;
			}
		};
		final List<Integer> buff2 = new ArrayList<>(100);
		int m = 0, k;
		while(true) {
			try {
				k = in.get(buff2, 100);
			} catch(final EOFException e) {
				break;
			}
			assertEquals(k, buff2.size());
			buff2.clear();
			m += k;
		}
		assertEquals(totalCount, m);
		assertEquals(m, doneCount.get());
	}
}