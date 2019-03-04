package com.emc.mongoose.perf;

import com.emc.mongoose.base.config.ConstantValueInputImpl;
import com.emc.mongoose.base.item.DataItemFactoryImpl;
import com.emc.mongoose.base.item.DataItemImpl;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.ItemNameSupplier;
import com.emc.mongoose.base.item.ItemNamingType;
import com.emc.mongoose.base.item.io.NewDataItemInput;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
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

  private static class CountingOutput<T> implements Output<T> {

    private final LongAdder counter;

    CountingOutput(final LongAdder counter) {
      this.counter = counter;
    }

    @Override
    public boolean put(final T item) {
      counter.increment();
      return true;
    }

    @Override
    public int put(final List<T> buffer, final int from, final int to) {
      counter.add(to - from);
      return to - from;
    }

    @Override
    public int put(final List<T> buffer) {
      counter.add(buffer.size());
      return buffer.size();
    }

    @Override
    public Input<T> getInput() {
      return null;
    }

    @Override
    public void close() throws Exception {}
  }

  private static final class RecyclingAndCountingOutput<T> extends CountingOutput<T> {

    LoadGenerator loadGenerator;

    RecyclingAndCountingOutput(final LongAdder counter) {
      super(counter);
    }

    @Override
    public boolean put(final T item) {
      loadGenerator.recycle((Operation) item);
      return super.put(item);
    }

    @Override
    public int put(final List<T> buffer, final int from, final int to) {
      for (int i = from; i < to; i++) {
        loadGenerator.recycle((Operation) buffer.get(i));
      }
      return super.put(buffer, from, to);
    }

    @Override
    public int put(final List<T> buffer) {
      for (int i = 0; i < buffer.size(); i++) {
        loadGenerator.recycle((Operation) buffer.get(i));
      }
      return super.put(buffer);
    }
  }

  @Test
  public final void testNewDataItems() throws Exception {

    final LongAdder counter = new LongAdder();

    final SizeInBytes itemSize = new SizeInBytes(0);
    final ItemFactory itemFactory = new DataItemFactoryImpl();
    final ItemNameSupplier itemNameInput =
        new ItemNameSupplier(ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0);
    final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.CREATE)
            .outputPathInput(new ConstantValueInputImpl<>("/default"))
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl<>(
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

    final LongAdder counter = new LongAdder();

    final SizeInBytes itemSize = new SizeInBytes(0);
    final ItemFactory itemFactory = new DataItemFactoryImpl();
    final ItemNameSupplier itemNameInput =
        new ItemNameSupplier(ItemNamingType.ASC, null, 10, 10, 0);
    final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.CREATE)
            .outputPathInput(new ConstantValueInputImpl<>("/default"))
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl(
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
    final ItemNameSupplier itemNameInput =
        new ItemNameSupplier(ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0);
    final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.CREATE)
            .outputPathInput(new ConstantValueInputImpl<>("/default"))
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl(
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
    final ItemNameSupplier itemNameInput =
        new ItemNameSupplier(ItemNamingType.RANDOM, null, 13, Character.MAX_RADIX, 0);
    final Input itemInput = new NewDataItemInput<>(itemFactory, itemNameInput, itemSize);
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.CREATE)
            // .outputPathInput(ExpressionInputBuilder.newInstance().type("$p{16;2}"))
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl(
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
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.READ)
            .outputPathInput(null)
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl(
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
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.READ)
            .outputPathInput(null)
            .credentialInput(null);
    final boolean shuffleFlag = true;

    try (final LoadGenerator loadGenerator =
        new LoadGeneratorImpl(
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
    final OperationsBuilder opsBuilder =
        new DataOperationsBuilderImpl(0)
            .opType(OpType.READ)
            .outputPathInput(null)
            .credentialInput(null);
    final boolean shuffleFlag = false;

    try (final Output taskOutput = new RecyclingAndCountingOutput(counter)) {
      try (final LoadGenerator loadGenerator =
          new LoadGeneratorImpl(
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
