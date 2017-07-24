package com.emc.mongoose.tests.perf;

import com.emc.mongoose.api.common.SizeInBytes;
import com.emc.mongoose.api.common.io.Input;
import com.emc.mongoose.api.common.io.Output;
import com.emc.mongoose.api.common.io.collection.CircularListInput;
import com.emc.mongoose.api.common.supply.ConstantStringSupplier;
import com.emc.mongoose.api.common.supply.RangePatternDefinedSupplier;
import com.emc.mongoose.load.generator.BasicLoadGenerator;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.io.task.IoTaskBuilder;
import com.emc.mongoose.api.model.io.task.data.BasicDataIoTaskBuilder;
import com.emc.mongoose.api.model.item.BasicDataItem;
import com.emc.mongoose.api.model.item.BasicDataItemFactory;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemNameSupplier;
import com.emc.mongoose.api.model.item.ItemNamingType;
import com.emc.mongoose.api.model.item.NewDataItemInput;
import com.emc.mongoose.api.model.load.LoadGenerator;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 30.03.17.
 */
public class BasicLoadGeneratorTest {
	
	private static final int BATCH_SIZE = 0x1000;
	
	private static final int TIME_LIMIT = 30;
	
	private static final class CountingOutput<T>
	implements Output<T> {
		
		private final LongAdder counter;
		
		public CountingOutput(final LongAdder counter) {
			this.counter = counter;
		}
		
		@Override
		public boolean put(final T item)
		throws IOException {
			counter.increment();
			return true;
		}
		
		@Override
		public int put(final List<T> buffer, final int from, final int to)
		throws IOException {
			counter.add(to - from);
			return to - from;
		}
		
		@Override
		public int put(final List<T> buffer)
		throws IOException {
			counter.add(buffer.size());
			return buffer.size();
		}
		
		@Override
		public Input<T> getInput()
		throws IOException {
			return null;
		}
		
		@Override
		public void close()
		throws IOException {
		}
	}
	
	@Test
	public final void testNewDataItems()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new BasicDataItemFactory();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.CREATE)
			.setOutputPathSupplier(new ConstantStringSupplier("/default"))
			.setUidSupplier(null)
			.setSecretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
		}
	}
	
	@Test
	public final void testNewDataItemsWithAscNames()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new BasicDataItemFactory();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.ASC, null, 10, 10, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.CREATE)
			.setOutputPathSupplier(new ConstantStringSupplier("/default"))
			.setUidSupplier(null)
			.setSecretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " (w/ asc names) rate: " + counter.sum() / TIME_LIMIT);
		}
	}
	
	@Test
	public final void testNewDataItemsWithCredentials()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new BasicDataItemFactory();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.CREATE)
			.setOutputPathSupplier(new ConstantStringSupplier("/default"))
			.setUidSupplier(new ConstantStringSupplier("wuser1@sanity.local"))
			.setSecretSupplier(new ConstantStringSupplier("secret"));
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + "(w/ constant credentials) rate: " + counter.sum() / TIME_LIMIT);
		}
	}
	
	@Test
	public final void testNewDataItemsWithDynamicPath()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new BasicDataItemFactory();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.CREATE)
			.setOutputPathSupplier(new RangePatternDefinedSupplier("$p{16;2}"))
			.setUidSupplier(null)
			.setSecretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " (w/ dynamic path) rate: " + counter.sum() / TIME_LIMIT);
		}
	}
	
	@Test
	public final void testDataItemsBuffer()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final List items = new ArrayList(BATCH_SIZE);
		BasicDataItem item;
		for(int i = 0; i < BATCH_SIZE; i ++) {
			item = new BasicDataItem();
			item.setName(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.READ)
			.setOutputPathSupplier(null)
			.setUidSupplier(null)
			.setSecretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
		}
	}
	
	@Test
	public final void testDataItemsBufferWithShuffle()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final List items = new ArrayList(BATCH_SIZE);
		BasicDataItem item;
		for(int i = 0; i < BATCH_SIZE; i ++) {
			item = new BasicDataItem();
			item.setName(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final IoTaskBuilder ioTaskBuilder = new BasicDataIoTaskBuilder()
			.setIoType(IoType.READ)
			.setOutputPathSupplier(null)
			.setUidSupplier(null)
			.setSecretSupplier(null);
		final boolean shuffleFlag = true;
		
		try(
			final LoadGenerator loadGenerator = new BasicLoadGenerator(
				itemInput, BATCH_SIZE, 0, ioTaskBuilder, Long.MAX_VALUE, new SizeInBytes(0),
				0, shuffleFlag
			)
		) {
			loadGenerator.setOutput(new CountingOutput(counter));
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " (w/ shuffling) rate: " + counter.sum() / TIME_LIMIT);
		}
	}
}
