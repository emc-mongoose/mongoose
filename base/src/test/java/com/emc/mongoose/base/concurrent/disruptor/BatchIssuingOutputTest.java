package com.emc.mongoose.base.concurrent.disruptor;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

public class BatchIssuingOutputTest {

	@Test
	public final void passEvents()
					throws Exception {
		final var dstBuff = (List<Integer>) new ArrayList<Integer>();
		final var buffSize = 128;
		final var handler = (Consumer<List<Integer>>) dstBuff::addAll;
		try (final var output = new BatchIssuingOutputImpl<Integer>(buffSize)) {
			output.register(handler);
			output.start();
			for (var i = 0; i < 1_000; i++) {
				while (!output.put(i)) {
					LockSupport.parkNanos(1);
				}
			}
			output.await();
		}
		for (var i = 0; i < 1_000; i++) {
			assertEquals(i, dstBuff.get(i).intValue());
		}
	}

	@Test
	public final void passBatchEvents()
					throws Exception {
		final var dstBuff = (List<Integer>) new ArrayList<Integer>();
		final var buffSize = 128;
		//System.out.println(batch.size());
		final var handler = (Consumer<List<Integer>>) dstBuff::addAll;
		try (final var output = new BatchIssuingOutputImpl<Integer>(buffSize)) {
			output.register(handler);
			output.start();
			final var srcBuff = (List<Integer>) new ArrayList<Integer>();
			for (var i = 0; i < 1_000; i++) {
				srcBuff.add(i);
			}
			int n;
			for (var done = 0; done < 1_000;) {
				n = output.put(srcBuff, done, 1_000);
				if (n == 0) {
					LockSupport.parkNanos(1);
				}
				done += n;
			}
			output.await();
		}
		for (var i = 0; i < 1_000; i++) {
			assertEquals(i, dstBuff.get(i).intValue());
		}
	}
}
