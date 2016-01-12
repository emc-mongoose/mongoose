package com.emc.mongoose.core.impl.item.data.model;
//
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.core.impl.item.base.LimitedQueueItemBuffer;
import org.junit.Test;
import static org.junit.Assert.*;
//
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
//
@RunWith(MockitoJUnitRunner.class)
public class LimitedQueueItemBufferTest {
	//
	final DataItem
		dataItem0 = Mockito.mock(DataItem.class),
		dataItem1 = Mockito.mock(DataItem.class),
		dataItem2 = Mockito.mock(DataItem.class),
		dataItem3 = Mockito.mock(DataItem.class),
		dataItem4 = Mockito.mock(DataItem.class);
	//
	@Test
	public void shouldReadSingleAfterSingleWrite()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(1)
		);
		itemsIO.put(dataItem0);
		assertEquals(dataItem0, itemsIO.get());
	}
	//
	@Test
	public void shouldReadInTheSameOrder()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(3)
		);
		itemsIO.put(dataItem0);
		itemsIO.put(dataItem1);
		itemsIO.put(dataItem2);
		assertEquals(dataItem0, itemsIO.get());
		assertEquals(dataItem1, itemsIO.get());
		assertEquals(dataItem2, itemsIO.get());
	}
	//
	@Test
	public void shouldReadBatchInTheSameOrder()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(5)
		);
		final List<DataItem>
			buffOut = Arrays.asList(
				new DataItem[] {dataItem0, dataItem1, dataItem2, dataItem3, dataItem4}
			),
			buffIn = new ArrayList<>(5);
		itemsIO.put(buffOut, 0, 5);
		assertEquals(5, itemsIO.get(buffIn, 5));
		assertEquals(buffIn, buffOut);
	}
	//
	@Test
	public void shouldFailWriteWhenOutOfCapacity()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.put(dataItem0);
		itemsIO.put(dataItem1);
		final Thread writeThread = new Thread() {
			@Override
			public void run() {
				try {
					itemsIO.put(dataItem2);
					fail();
				} catch(final IOException e) {
					assertNotNull(e);
				}
			}
		};
		writeThread.start();
		TimeUnit.SECONDS.timedJoin(writeThread, 1);
		writeThread.interrupt();
	}
	//
	@Test
	public void shouldWriteBatchPartiallyWhenOutOfCapacity()
	throws Exception {
		final BlockingQueue<DataItem> queue = new ArrayBlockingQueue<>(2);
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(queue);
		final int n = itemsIO.put(
			Arrays.asList(dataItem0, dataItem1, dataItem2, dataItem3, dataItem4), 0, 5
		);
		assertEquals(2, n);
	}
	//
	@Test
	public void shouldAllowWriteWhenRead()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.put(dataItem0);
		itemsIO.put(dataItem1);
		assertEquals(dataItem0, itemsIO.get());
		itemsIO.put(dataItem2);
		assertEquals(dataItem1, itemsIO.get());
		assertEquals(dataItem2, itemsIO.get());
	}
	//
	@Test
	public void shouldReadNullWhenEmpty()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.put(dataItem0);
		itemsIO.put(dataItem1);
		assertEquals(itemsIO.get(), dataItem0);
		assertEquals(itemsIO.get(), dataItem1);
		assertNull(itemsIO.get());
	}
	//
	@Test
	public void shouldAllowReadWhenWritten()
	throws Exception {
		final LimitedQueueItemBuffer<DataItem> itemsIO = new LimitedQueueItemBuffer<>(
			new ArrayBlockingQueue<DataItem>(3)
		);
		itemsIO.put(dataItem0);
		itemsIO.put(dataItem1);
		itemsIO.put(dataItem2);
		assertEquals(dataItem0, itemsIO.get());
		itemsIO.put(dataItem3);
		assertEquals(dataItem1, itemsIO.get());
		itemsIO.put(dataItem4);
		assertEquals(dataItem2, itemsIO.get());
		assertEquals(dataItem3, itemsIO.get());
		assertEquals(dataItem4, itemsIO.get());
	}
}
