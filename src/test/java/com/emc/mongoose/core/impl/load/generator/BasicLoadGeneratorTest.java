package com.emc.mongoose.core.impl.load.generator;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.LoadGenerator;
import com.emc.mongoose.core.api.load.metrics.IoStats;
import com.emc.mongoose.core.impl.item.base.BasicItem;
import com.emc.mongoose.core.impl.item.base.ListItemInput;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
/**
 Created by kurila on 13.04.16.
 */
public class BasicLoadGeneratorTest {

	private final List<Item> items = Arrays.<Item>asList(
		new BasicItem("0"),
		new BasicItem("1"),
		new BasicItem("2"),
		new BasicItem("3"),
		new BasicItem("4"),
		new BasicItem("5"),
		new BasicItem("6"),
		new BasicItem("7"),
		new BasicItem("8"),
		new BasicItem("9"),
		new BasicItem("a"),
		new BasicItem("b"),
		new BasicItem("c"),
		new BasicItem("d"),
		new BasicItem("e"),
		new BasicItem("f"),
		new BasicItem("g"),
		new BasicItem("h"),
		new BasicItem("i"),
		new BasicItem("j"),
		new BasicItem("k"),
		new BasicItem("l"),
		new BasicItem("m"),
		new BasicItem("n"),
		new BasicItem("o"),
		new BasicItem("p"),
		new BasicItem("q"),
		new BasicItem("r"),
		new BasicItem("s"),
		new BasicItem("t"),
		new BasicItem("u"),
		new BasicItem("v"),
		new BasicItem("w"),
		new BasicItem("x"),
		new BasicItem("y"),
		new BasicItem("z")
	);

	private final Input<Item> itemInput = new ListItemInput<>(items);

	private final LoadExecutor<Item, IoTask<Item>>
		loadExecutorMock = new LoadExecutor<Item, IoTask<Item>>() {
			@Override
			public int submit(
				final LoadGenerator<Item, IoTask<Item>> loadGenerator,
				final List<IoTask<Item>> ioTasks, final int from, final int to
			) throws IOException {
				return 0;
			}
			@Override
			public void close()
			throws IOException {
			}
		};

	@Test
	public void testCount()
	throws Exception {
		try (
			final LoadGenerator<Item, IoTask<Item>> loadGenerator = new BasicLoadGenerator<>(
				BasicConfig.THREAD_CONTEXT.get(), "testLoadGenerator", LoadType.WRITE,
				loadExecutorMock, itemInput, 0, 0, 0, false, false, null
			)
		) {
			loadGenerator.start();
			loadGenerator.await();
			final IoStats.Snapshot lastStats = loadGenerator.getStatsSnapshot();
			Assert.assertEquals(items.size(), lastStats.getSuccCount());
		}
	}
}
