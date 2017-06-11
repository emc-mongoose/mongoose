package com.emc.mongoose.tests.perf;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.model.item.CsvFileItemInput;
import com.emc.mongoose.model.item.CsvFileItemOutput;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemNameSupplier;
import com.emc.mongoose.model.item.ItemNamingType;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.item.NewDataItemInput;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.EOFException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

import static org.junit.Assert.assertEquals;

/**
 Created by andrey on 11.06.17.
 */
public class CsvFileItemInputTest {

	private static final int BATCH_SIZE = 0x1000;
	private static final String FILE_NAME = "items.csv";

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		try {
			Files.delete(Paths.get(FILE_NAME));
		} catch(final Exception ignored) {
		}
	}

	@Test
	public final void testInputRate()
	throws Exception {

		final long count = 100_000_000;

		final ItemFactory<DataItem> itemFactory = ItemType.getItemFactory(ItemType.DATA);
		final List<DataItem> itemBuff = new ArrayList<>(BATCH_SIZE);

		try(
			final Input<DataItem> newItemsInput = new NewDataItemInput<>(
				ItemType.getItemFactory(ItemType.DATA),
				new ItemNameSupplier(ItemNamingType.ASC, "", 13, Character.MAX_RADIX, 0),
				new SizeInBytes("0-1MB,2")
			)
		) {
			try(
				final Output<DataItem> newItemsOutput = new CsvFileItemOutput<DataItem>(
					Paths.get(FILE_NAME), itemFactory
				)
			) {
				long n = 0;
				int m;
				while(n < count) {
					m = newItemsInput.get(itemBuff, BATCH_SIZE);
					for(int i = 0; i < m; i += newItemsOutput.put(itemBuff, i, m));
					n += m;
					itemBuff.clear();
				}
			}
		}

		System.out.println("Items input file is ready, starting the input");
		final LongAdder inputCounter = new LongAdder();
		long t = System.nanoTime();
		try(
			final Input<DataItem> fileItemInput = new CsvFileItemInput<DataItem>(
				Paths.get(FILE_NAME), itemFactory
			)
		) {
			int n;
			while(true) {
				n = fileItemInput.get(itemBuff, BATCH_SIZE);
				if(n > 0) {
					inputCounter.add(n);
					itemBuff.clear();
				} else {
					break;
				}
			}
		} catch(final EOFException ignored) {
		}
		t = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - t);

		assertEquals(count, inputCounter.sum(), BATCH_SIZE);
		System.out.println("CSV file input rate: " + count/t + " items per second");
	}
}
