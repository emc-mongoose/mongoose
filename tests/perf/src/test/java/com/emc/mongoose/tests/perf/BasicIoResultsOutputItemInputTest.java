package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.supply.ConstantStringSupplier;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.io.task.data.BasicDataIoTaskBuilder;
import com.emc.mongoose.model.io.task.data.DataIoTask;
import com.emc.mongoose.model.item.BasicIoResultsOutputItemInput;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.IoResultsOutputItemInput;
import com.emc.mongoose.model.item.ItemNameSupplier;
import com.emc.mongoose.model.item.ItemNamingType;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.item.NewDataItemInput;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.EOFException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.fail;

/**
 Created by andrey on 11.06.17.
 */
public class BasicIoResultsOutputItemInputTest {

	private static final int BATCH_SIZE = 0x1000;
	private static final int BUFF_CAPACITY = 1_000_000;
	private static Input<DataItem> ITEM_INPUT;
	private static IoTaskBuilder<DataItem, DataIoTask<DataItem>> IO_TASK_BUILDER;
	private static IoResultsOutputItemInput<DataItem, DataIoTask<DataItem>> BUFF;
	private static final int TIMEOUT = 100;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ITEM_INPUT = new NewDataItemInput<>(
			ItemType.getItemFactory(ItemType.DATA),
			new ItemNameSupplier(ItemNamingType.ASC, null, 13, Character.MAX_RADIX, 0),
			new SizeInBytes(0)
		);
		IO_TASK_BUILDER = new BasicDataIoTaskBuilder<>();
		IO_TASK_BUILDER.setIoType(IoType.NOOP);
		IO_TASK_BUILDER.setOutputPathSupplier(new ConstantStringSupplier("/default"));
		IO_TASK_BUILDER.setUidSupplier(new ConstantStringSupplier("uid1"));
		IO_TASK_BUILDER.setSecretSupplier(new ConstantStringSupplier("secret1"));
		BUFF = new BasicIoResultsOutputItemInput<>(BUFF_CAPACITY, TimeUnit.SECONDS, 0);
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		IO_TASK_BUILDER.close();
		BUFF.close();
	}

	@Test
	public final void testIoRate()
	throws Exception {
		final LongAdder OP_COUNTER = new LongAdder();
		final Thread producerThread = new Thread(
			() -> {
				final List<DataItem> dataItemsBuff = new ArrayList<>(BATCH_SIZE);
				final List<DataIoTask<DataItem>> ioTaskBuff = new ArrayList<>(BATCH_SIZE);
				try {
					while(true) {
						if(BATCH_SIZE != ITEM_INPUT.get(dataItemsBuff, BATCH_SIZE)) {
							fail();
						}
						IO_TASK_BUILDER.getInstances(dataItemsBuff, ioTaskBuff);
						for(int n = 0; n < BATCH_SIZE; n += BUFF.put(ioTaskBuff, n, BATCH_SIZE));
						dataItemsBuff.clear();
						ioTaskBuff.clear();
					}
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		);
		final Thread consumerThread = new Thread(
			() -> {
				final List<DataItem> dataItemsBuff = new ArrayList<>(BATCH_SIZE);
				int n;
				try {
					while(true) {
						n = BUFF.get(dataItemsBuff, BATCH_SIZE);
						if(n < 0) {
							break;
						} else {
							OP_COUNTER.add(n);
							dataItemsBuff.clear();
						}
					}
				} catch(final EOFException ignored) {
				} catch(final Exception e) {
					e.printStackTrace(System.err);
				}
			}
		);
		producerThread.start();
		consumerThread.start();
		TimeUnit.SECONDS.sleep(TIMEOUT);
		producerThread.interrupt();
		consumerThread.interrupt();
		System.out.println("I/O rate: " + OP_COUNTER.sum() / TIMEOUT);
	}
}
