package com.emc.mongoose.core.impl.data.model;

import com.emc.mongoose.core.api.data.DataItem;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by kirill_gusakov on 06.09.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class BinItemInputTest {

	@Mock
	private ObjectInputStream itemSrc;

	@Test
	public void shouldReadSingleDataItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		Mockito.when(itemSrc.readUnshared())
			.thenReturn(dataItem)
			.thenThrow(new RuntimeException());

		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);
		Assert.assertEquals(itemInput.get(), dataItem);
	}

	@Test
	public void shouldReadSingleDataItemToBuffer()
	throws Exception {
		final DataItem dataItems[] = new DataItem[] {
			Mockito.mock(DataItem.class)
		};
		Mockito.when(itemSrc.readUnshared())
			.thenReturn(dataItems[dataItems.length - 1])
			.thenThrow(new RuntimeException());

		int maxCount = 5;
		final List<DataItem> buffer = new ArrayList<>(maxCount);

		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);

		Assert.assertEquals(itemInput.get(buffer, maxCount), dataItems.length);
		Assert.assertEquals(buffer, Arrays.asList(dataItems));
	}

	@Test
	public void shouldReadListOfDataItemsToBuffer()
	throws Exception {
		final DataItem dataItems[] = new DataItem[] {
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
		};

		Mockito.when(itemSrc.readUnshared())
			.thenReturn(Arrays.asList(dataItems))
			.thenThrow(new RuntimeException());

		int maxCount = 5;
		final List<DataItem> buffer = new ArrayList<>(maxCount);

		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);

		Assert.assertEquals(itemInput.get(buffer, maxCount), dataItems.length);
		Assert.assertEquals(buffer, Arrays.asList(dataItems));
	}

	@Test
	public void shouldReadRemainingDataItemsToBuffer()
	throws Exception {
		final DataItem dataItems[] = new DataItem[] {
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
		};

		Mockito.when(itemSrc.readUnshared())
			.thenReturn(Arrays.asList(dataItems))
			.thenThrow(new RuntimeException());

		int startCount = 2;
		final List<DataItem> buffer = new ArrayList<>(startCount);

		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);

		Assert.assertEquals(itemInput.get(buffer, startCount), startCount);
		Assert.assertEquals(buffer, Arrays.asList(dataItems).subList(0, startCount));

		int remainingCount = 3;
		final List<DataItem> remainingBuf = new ArrayList<>(remainingCount);

		Assert.assertEquals(itemInput.get(remainingBuf, remainingCount), remainingCount);
		Assert.assertEquals(remainingBuf, Arrays.asList(dataItems)
			.subList(startCount, startCount + remainingCount));
	}

	@Test(expected = IOException.class)
	public void shouldThrowExceptionAfterStreamReset()
	throws Exception {
		Mockito.doThrow(new IOException()).when(itemSrc).reset();

		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);
		itemInput.reset();
	}

	@Test
	public void shouldSkipSomeDataItems()
	throws Exception {
		final DataItem dataItems[] = new DataItem[] {
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
		};

		int i = 0;
		Mockito.when(itemSrc.readUnshared())
			.thenReturn(dataItems[i++])
			.thenReturn(dataItems[i++])
			.thenReturn(dataItems[i++])
			.thenReturn(dataItems[i++])
			.thenReturn(dataItems[i])
			.thenThrow(new RuntimeException());

		final int countOfSkippedItems = 3;
		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);
		itemInput.skip(countOfSkippedItems);

		Assert.assertEquals(itemInput.get(), dataItems[countOfSkippedItems]);
	}

	@Test(expected = IOException.class)
	public void shouldThrowIOExceptionIfNoSuchAmountOfDataItems()
	throws Exception {
		final DataItem dataItems[] = new DataItem[] {
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
		};

		int i = 0;
		Mockito.when(itemSrc.readUnshared())
				.thenReturn(dataItems[i++])
				.thenReturn(dataItems[i++])
				.thenReturn(dataItems[i++])
				.thenReturn(dataItems[i++])
				.thenReturn(dataItems[i])
				.thenThrow(new IOException());

		final int countOfSkippedItems = 6;
		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);
		itemInput.skip(countOfSkippedItems);
	}

	@Test
	public void shouldClose()
	throws Exception {
		final BinItemSrc<DataItem> itemInput = new BinItemSrc<>(itemSrc);
		itemInput.close();

		Mockito.verify(itemSrc).close();
		Mockito.verifyNoMoreInteractions(itemSrc);
	}

}
