package com.emc.mongoose.tests.perf;

import com.emc.mongoose.api.common.concurrent.ThreadUtil;
import com.emc.mongoose.storage.driver.net.base.pool.ConnLeaseException;
import com.emc.mongoose.storage.driver.net.base.pool.NonBlockingConnPool;
import com.emc.mongoose.tests.perf.util.mock.BasicMultiNodeConnPoolMock;
import com.emc.mongoose.tests.perf.util.mock.DummyChannelPoolHandlerMock;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by andrey on 12.05.17.
 */
@RunWith(Parameterized.class)
public class BasicMultiNodeConnPoolTest {

	private static final int TEST_STEP_TIME_SECONDS = 100;
	private static final int BATCH_SIZE = 0x1000;

	private int concurrencyLevel;
	private int nodeCount;

	@Parameterized.Parameters
	public static Collection<Object[]> generateData() {
		return Arrays.asList(
			new Object[][] {
				{1, 1},
				{10, 1}, {10, 10},
				{100, 1}, {100, 10},
				{1000, 1}, {1000, 10}
			}
		);
	}

	public BasicMultiNodeConnPoolTest(final int concurrencyLevel, final int nodeCount) {
		this.concurrencyLevel = concurrencyLevel;
		this.nodeCount = nodeCount;
		final String[] nodes = new String[nodeCount];
		for(int i = 0; i < nodeCount; i ++) {
			nodes[i] = Integer.toString(i);
		}
		final LongAdder opCount = new LongAdder();
		try(
			final NonBlockingConnPool connPool = new BasicMultiNodeConnPoolMock(
				concurrencyLevel, new Semaphore(concurrencyLevel), nodes, new Bootstrap(),
				new DummyChannelPoolHandlerMock(), 9020, 0
			)
		) {
			final ExecutorService poolLoader = Executors.newFixedThreadPool(
				ThreadUtil.getHardwareThreadCount()
			);
			for(int i = 0; i < ThreadUtil.getHardwareThreadCount(); i ++) {
				poolLoader.submit(
					() -> {
						final Thread currThread = Thread.currentThread();
						final List<Channel> connBuff = new ArrayList<>(BATCH_SIZE);
						int j, k;
						Channel c;
						try {
							while(!currThread.isInterrupted()) {
								for(j = 0; j < BATCH_SIZE; j ++) {
									c = connPool.lease();
									if(c == null) {
										break;
									}
									connBuff.add(c);
								}
								for(k = 0; k < j; k ++) {
									connPool.release(connBuff.get(k));
									opCount.increment();
								}
								connBuff.clear();
							}
						} catch(final ConnLeaseException ignored) {
						}
					}
				);
			}
			poolLoader.shutdown();
			try {
				poolLoader.awaitTermination(TEST_STEP_TIME_SECONDS, TimeUnit.SECONDS);
			} catch(final InterruptedException e) {
				e.printStackTrace();
			}
			poolLoader.shutdownNow();
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		} finally {
			System.out.println(
				"concurrency = " + concurrencyLevel + ", nodes = " + nodeCount + " -> rate: " +
					opCount.sum() / TEST_STEP_TIME_SECONDS
			);
		}
	}

	@Test
	public void test() {
		assertTrue(true);
	}
}
