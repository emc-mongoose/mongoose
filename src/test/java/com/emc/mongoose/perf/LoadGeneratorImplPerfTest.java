package com.emc.mongoose.perf;

import com.emc.mongoose.load.generator.LoadGenerator;
import com.emc.mongoose.load.generator.LoadGeneratorImpl;
import com.github.akurilov.commons.io.collection.ListInput;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.io.collection.CircularListInput;

import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.supply.ConstantStringSupplier;
import com.emc.mongoose.supply.RangePatternDefinedSupplier;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.OperationsBuilder;
import com.emc.mongoose.item.op.data.DataOperationsBuilderImpl;
import com.emc.mongoose.item.DataItemImpl;
import com.emc.mongoose.item.DataItemFactoryImpl;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemNameSupplier;
import com.emc.mongoose.item.ItemNamingType;
import com.emc.mongoose.item.io.NewDataItemInput;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

/**
 Created by kurila on 30.03.17.
 */
public class LoadGeneratorImplPerfTest {
	
	private static final int BATCH_SIZE = 0x1000;
	
	private static final int TIME_LIMIT = 30;
	
	private static class CountingOutput<T>
	implements Output<T> {
		
		private final LongAdder counter;
		
		CountingOutput(final LongAdder counter) {
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

	private static final class RecyclingAndCountingOutput<T>
	extends CountingOutput<T> {

		LoadGenerator loadGenerator;

		RecyclingAndCountingOutput(final LongAdder counter) {
			super(counter);
		}

		@Override
		public boolean put(final T item)
		throws IOException {
			loadGenerator.recycle((Operation) item);
			return super.put(item);
		}

		@Override
		public int put(final List<T> buffer, final int from, final int to)
		throws IOException {
			for(int i = from; i < to; i ++) {
				loadGenerator.recycle((Operation) buffer.get(i));
			}
			return super.put(buffer, from, to);
		}

		@Override
		public int put(final List<T> buffer)
		throws IOException {
			for(int i = 0; i < buffer.size(); i ++) {
				loadGenerator.recycle((Operation) buffer.get(i));
			}
			return super.put(buffer);
		}
	}
	
	@Test
	public final void testNewDataItems()
	throws Exception {
		
		final LongAdder counter = new LongAdder();
		
		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput<>(
			itemFactory, itemNameInput, itemSize
		);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.CREATE)
			.outputPathSupplier(new ConstantStringSupplier("/default"))
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl<>(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
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
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.ASC, null, 10, 10, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.CREATE)
			.outputPathSupplier(new ConstantStringSupplier("/default"))
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter),BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
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
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.CREATE)
			.outputPathSupplier(new ConstantStringSupplier("/default"))
			.uidSupplier(new ConstantStringSupplier("wuser1@sanity.local"))
			.secretSupplier(new ConstantStringSupplier("secret"));
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
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
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final ItemNameSupplier itemNameInput = new ItemNameSupplier(
			ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0
		);
		final Input itemInput = new NewDataItemInput(itemFactory, itemNameInput, itemSize);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.CREATE)
			.outputPathSupplier(new RangePatternDefinedSupplier("$p{16;2}"))
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
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
		DataItemImpl item;
		for(int i = 0; i < BATCH_SIZE; i ++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.READ)
			.outputPathSupplier(null)
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = false;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
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
		DataItemImpl item;
		for(int i = 0; i < BATCH_SIZE; i ++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.READ)
			.outputPathSupplier(null)
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = true;
		
		try(
			final LoadGenerator loadGenerator = new LoadGeneratorImpl(
				itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
				Long.MAX_VALUE, 1, false, shuffleFlag
			)
		) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " (w/ shuffling) rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testRecycleMode()
	throws Exception {

		final LongAdder counter = new LongAdder();

		final List items = new ArrayList(BATCH_SIZE);
		DataItemImpl item;
		for(int i = 0; i < BATCH_SIZE; i ++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new ListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
			.opType(OpType.READ)
			.outputPathSupplier(null)
			.uidSupplier(null)
			.secretSupplier(null);
		final boolean shuffleFlag = false;

		try(final Output taskOutput = new RecyclingAndCountingOutput(counter)) {
			try(
				final LoadGenerator loadGenerator = new LoadGeneratorImpl(
					itemInput, opsBuilder, Collections.emptyList(), new CountingOutput(counter), BATCH_SIZE,
					Long.MAX_VALUE, 1_000_000, true, shuffleFlag
				)
			) {
				((RecyclingAndCountingOutput) taskOutput).loadGenerator = loadGenerator;
				loadGenerator.start();
				TimeUnit.SECONDS.sleep(TIME_LIMIT);
				System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
			}
		}
	}
}
