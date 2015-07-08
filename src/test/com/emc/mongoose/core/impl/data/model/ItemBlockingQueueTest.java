package com.emc.mongoose.core.impl.data.model;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import org.junit.Test;
import static org.junit.Assert.*;
//
import org.mockito.Mockito;
//
import java.io.InterruptedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
//
public class ItemBlockingQueueTest {
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
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(1)
		);
		itemsIO.write(dataItem0);
		assertEquals(dataItem0, itemsIO.read());
	}
	//
	@Test
	public void shouldReadInTheSameOrder()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(3)
		);
		itemsIO.write(dataItem0);
		itemsIO.write(dataItem1);
		itemsIO.write(dataItem2);
		assertEquals(dataItem0, itemsIO.read());
		assertEquals(dataItem1, itemsIO.read());
		assertEquals(dataItem2, itemsIO.read());
	}
	//
	@Test
	public void shouldReadBatchInTheSameOrder()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(5)
		);
		final List<DataItem>
			buffOut = Arrays.asList(
				new DataItem[] {dataItem0, dataItem1, dataItem2, dataItem3, dataItem4}
			),
			buffIn = new ArrayList<>(5);
		itemsIO.write(buffOut);
		assertEquals(5, itemsIO.read(buffIn));
		assertEquals(buffIn, buffOut);
	}
	//
	@Test
	public void shouldBlockWriteWhenOutOfCapacity()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.write(dataItem0);
		itemsIO.write(dataItem1);
		final Thread writeThread = new Thread() {
			@Override
			public void run() {
				try {
					itemsIO.write(dataItem2);
					fail();
				} catch(final InterruptedIOException e) {
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
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(queue);
		final int n = itemsIO.write(
			Arrays.asList(
				new DataItem[] {dataItem0, dataItem1, dataItem2, dataItem3, dataItem4}
			)
		);
		assertEquals(2, n);
	}
	//
	@Test
	public void shouldUnblockWriteWhenRead()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.write(dataItem0);
		itemsIO.write(dataItem1);
		final Thread writeThread = new Thread() {
			@Override
			public void run() {
				try {
					itemsIO.write(dataItem2);
				} catch(final InterruptedIOException e) {
					fail(e.getMessage());
				}
			}
		};
		writeThread.start();
		TimeUnit.SECONDS.sleep(1);
		assertEquals(dataItem0, itemsIO.read());
		assertEquals(dataItem1, itemsIO.read());
		assertEquals(dataItem2, itemsIO.read());
		TimeUnit.SECONDS.timedJoin(writeThread, 1);
		writeThread.interrupt();
	}
	//
	@Test
	public void shouldBlockReadWhenEmpty()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(2)
		);
		itemsIO.write(dataItem0);
		itemsIO.write(dataItem1);
		assertEquals(itemsIO.read(), dataItem0);
		assertEquals(itemsIO.read(), dataItem1);
		final Thread readThread = new Thread() {
			@Override
			public void run() {
				try {
					itemsIO.read();
					fail();
				} catch(final InterruptedIOException e) {
					assertNotNull(e);
				}
			}
		};
		readThread.start();
		TimeUnit.SECONDS.timedJoin(readThread, 1);
		readThread.interrupt();
	}
	//
	@Test
	public void shouldUnblockReadWhenWritten()
	throws Exception {
		final ItemBlockingQueue<DataItem> itemsIO = new ItemBlockingQueue<>(
			new ArrayBlockingQueue<DataItem>(3)
		);
		itemsIO.write(dataItem0);
		itemsIO.write(dataItem1);
		itemsIO.write(dataItem2);
		final Thread readThread = new Thread() {
			@Override
			public void run() {
				try {
					assertEquals(dataItem0, itemsIO.read());
					assertEquals(dataItem1, itemsIO.read());
					assertEquals(dataItem2, itemsIO.read());
					assertEquals(dataItem3, itemsIO.read());
					assertEquals(dataItem4, itemsIO.read());
				} catch(final InterruptedIOException e) {
					fail(e.getMessage());
				}
			}
		};
		readThread.start();
		TimeUnit.SECONDS.sleep(1);
		itemsIO.write(dataItem3);
		itemsIO.write(dataItem4);
		TimeUnit.SECONDS.timedJoin(readThread, 1);
		readThread.interrupt();
	}
}
