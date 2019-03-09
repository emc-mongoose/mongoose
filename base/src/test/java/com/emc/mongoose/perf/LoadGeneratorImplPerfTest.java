package com.emc.mongoose.perf;

import com.emc.mongoose.base.config.ConstantValueInputImpl;
import com.emc.mongoose.base.config.el.CompositeExpressionInputBuilder;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.DataItemFactoryImpl;
import com.emc.mongoose.base.item.DataItemImpl;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.naming.ItemNameInput;
import com.emc.mongoose.base.item.io.NewDataItemInput;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.OperationsBuilder;
import com.emc.mongoose.base.item.op.data.DataOperationsBuilderImpl;
import com.emc.mongoose.base.load.generator.LoadGenerator;
import com.emc.mongoose.base.load.generator.LoadGeneratorImpl;
import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.Output;
import com.github.akurilov.commons.io.collection.CircularListInput;
import com.github.akurilov.commons.io.collection.ListInput;
import com.github.akurilov.commons.system.SizeInBytes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import org.junit.Test;

/** Created by kurila on 30.03.17. */
public class LoadGeneratorImplPerfTest {

	private static final int BATCH_SIZE = 0x1000;

	private static final int TIME_LIMIT = 30;

	@Test
	public final void testNewDataItems() throws Exception {

		final var counter = new LongAdder();

		final var itemSize = new SizeInBytes(0);
		final var itemFactory = (ItemFactory<DataItem>) new DataItemFactoryImpl();
		final var itemNameInput = (ItemNameInput) ItemNameInput.Builder.newInstance()
						.type(ItemNameInput.ItemNamingType.RANDOM)
						.radix(Character.MAX_RADIX)
						.length(12)
						.build();
		final var itemInput = (Input) new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
		final var opsBuilder = (OperationsBuilder) new DataOperationsBuilderImpl(0)
						.opType(OpType.CREATE)
						.outputPathInput(new ConstantValueInputImpl<>("/default"))
						.credentialInput(null);
		final var shuffleFlag = false;

		try (final var loadGenerator = (LoadGenerator) new LoadGeneratorImpl<>(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testNewDataItemsWithAscNames() throws Exception {

		final var counter = new LongAdder();
		final var itemSize = new SizeInBytes(0);
		final var itemFactory = (ItemFactory) new DataItemFactoryImpl();
		final var itemNameInput = (ItemNameInput) ItemNameInput.Builder.newInstance()
						.type(ItemNameInput.ItemNamingType.SERIAL)
						.radix(10)
						.length(10)
						.build();
		final var itemInput = (Input) new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
		final var opsBuilder = (OperationsBuilder) new DataOperationsBuilderImpl(0)
						.opType(OpType.CREATE)
						.outputPathInput(new ConstantValueInputImpl<>("/default"))
						.credentialInput(null);
		final var shuffleFlag = false;

		try (final var loadGenerator = (LoadGenerator) new LoadGeneratorImpl(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(
							loadGenerator.toString() + " (w/ asc names) rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testNewDataItemsWithCredentials() throws Exception {

		final LongAdder counter = new LongAdder();

		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final var itemNameInput = (ItemNameInput) ItemNameInput.Builder.newInstance()
						.type(ItemNameInput.ItemNamingType.RANDOM)
						.radix(Character.MAX_RADIX)
						.length(12)
						.build();
		final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
						.opType(OpType.CREATE)
						.outputPathInput(new ConstantValueInputImpl<>("/default"))
						.credentialInput(null);
		final boolean shuffleFlag = false;

		try (final LoadGenerator loadGenerator = new LoadGeneratorImpl(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(
							loadGenerator.toString()
											+ "(w/ constant credentials) rate: "
											+ counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testNewDataItemsWithDynamicPath() throws Exception {

		final LongAdder counter = new LongAdder();

		final SizeInBytes itemSize = new SizeInBytes(0);
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final var itemNameInput = (ItemNameInput) ItemNameInput.Builder.newInstance()
						.type(ItemNameInput.ItemNamingType.RANDOM)
						.radix(Character.MAX_RADIX)
						.length(12)
						.build();
		final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
		final Input pathInput = CompositeExpressionInputBuilder.newInstance()
						.expression("${path:random(16, 2)}")
						.build();
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
						.opType(OpType.CREATE)
						.outputPathInput(pathInput)
						.credentialInput(null);
		final boolean shuffleFlag = false;

		try (final LoadGenerator loadGenerator = new LoadGeneratorImpl(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(
							loadGenerator.toString() + " (w/ dynamic path) rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testDataItemsBuffer() throws Exception {

		final LongAdder counter = new LongAdder();

		final List items = new ArrayList(BATCH_SIZE);
		DataItemImpl item;
		for (int i = 0; i < BATCH_SIZE; i++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
						.opType(OpType.READ)
						.outputPathInput(null)
						.credentialInput(null);
		final boolean shuffleFlag = false;

		try (final LoadGenerator loadGenerator = new LoadGeneratorImpl(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testDataItemsBufferWithShuffle() throws Exception {

		final LongAdder counter = new LongAdder();

		final List items = new ArrayList(BATCH_SIZE);
		DataItemImpl item;
		for (int i = 0; i < BATCH_SIZE; i++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new CircularListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
						.opType(OpType.READ)
						.outputPathInput(null)
						.credentialInput(null);
		final boolean shuffleFlag = true;

		try (final LoadGenerator loadGenerator = new LoadGeneratorImpl(
						itemInput,
						opsBuilder,
						Collections.emptyList(),
						new CountingOutput(counter),
						BATCH_SIZE,
						Long.MAX_VALUE,
						1,
						false,
						shuffleFlag)) {
			loadGenerator.start();
			TimeUnit.SECONDS.sleep(TIME_LIMIT);
			System.out.println(
							loadGenerator.toString() + " (w/ shuffling) rate: " + counter.sum() / TIME_LIMIT);
		}
	}

	@Test
	public final void testRecycleMode() throws Exception {

		final LongAdder counter = new LongAdder();

		final List items = new ArrayList(BATCH_SIZE);
		DataItemImpl item;
		for (int i = 0; i < BATCH_SIZE; i++) {
			item = new DataItemImpl();
			item.name(Long.toString(System.nanoTime(), Character.MAX_RADIX));
			items.add(item);
		}
		final Input itemInput = new ListInput(items);
		final OperationsBuilder opsBuilder = new DataOperationsBuilderImpl(0)
						.opType(OpType.READ)
						.outputPathInput(null)
						.credentialInput(null);
		final boolean shuffleFlag = false;

		try (final Output taskOutput = new RecyclingAndCountingOutput(counter)) {
			try (final LoadGenerator loadGenerator = new LoadGeneratorImpl(
							itemInput,
							opsBuilder,
							Collections.emptyList(),
							new CountingOutput(counter),
							BATCH_SIZE,
							Long.MAX_VALUE,
							1_000_000,
							true,
							shuffleFlag)) {
				((RecyclingAndCountingOutput) taskOutput).loadGenerator = loadGenerator;
				loadGenerator.start();
				TimeUnit.SECONDS.sleep(TIME_LIMIT);
				System.out.println(loadGenerator.toString() + " rate: " + counter.sum() / TIME_LIMIT);
			}
		}
	}
}
