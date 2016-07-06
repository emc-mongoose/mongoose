package com.emc.mongoose.unit;
//
import com.emc.mongoose.core.api.item.data.DataItem;
//
import static org.junit.Assert.*;

import com.emc.mongoose.core.impl.item.base.ListItemInput;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
//
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//
@RunWith(MockitoJUnitRunner.class)
public class ListItemInputTest {
	//
	@Mock private List<DataItem> itemsSrc;
	@Rule public ExpectedException thrown = ExpectedException.none();
	//
	@Test
	public void shouldReadSingleItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		Mockito.when(itemsSrc.size()).thenReturn(1);
		Mockito.when(itemsSrc.get(0)).thenReturn(dataItem);
		//
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		assertEquals(itemsInput.get(), dataItem);
	}
	//
	@Test(expected = EOFException.class)
	public void shouldThrowEOFIfNoMoreItems()
	throws Exception {
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		assertNull(itemsInput.get());
	}
	//
	@Test
	public void shouldReadManyItemsToBuffer()
	throws Exception {
		final DataItem
			dataItems[] = new DataItem[] {
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class)
		};
		final List<DataItem> buffer = new ArrayList<>(dataItems.length);
		Mockito.when(itemsSrc.size()).thenReturn(dataItems.length);
		Mockito
			.when(itemsSrc.subList(0, dataItems.length))
			.thenReturn(Arrays.asList(dataItems));
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		assertEquals(itemsInput.get(buffer, dataItems.length), dataItems.length);
	}
	//
	@Test
	public void shouldReadAllItemsToLargeBuffer()
	throws Exception {
		final DataItem
			dataItems[] = new DataItem[] {
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class)
			};
		final List<DataItem> buffer = new ArrayList<>(10);
		Mockito.when(itemsSrc.size()).thenReturn(dataItems.length);
		Mockito
			.when(itemsSrc.subList(0, dataItems.length))
			.thenReturn(Arrays.asList(dataItems));
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		assertEquals(itemsInput.get(buffer, dataItems.length), dataItems.length);
	}
	//
	@Test
	public void shouldReadAgainAfterReset()
	throws IOException {
		final DataItem
			dataItems[] = new DataItem[] {
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class),
			Mockito.mock(DataItem.class)
		};
		final List<DataItem> buffer = new ArrayList<>(3);
		Mockito.when(itemsSrc.size()).thenReturn(dataItems.length);
		Mockito
			.when(itemsSrc.subList(0, dataItems.length))
			.thenReturn(Arrays.asList(dataItems));
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		assertEquals(itemsInput.get(buffer, dataItems.length), dataItems.length);
		assertEquals(buffer.size(), dataItems.length);
		itemsInput.reset();
		assertEquals(itemsInput.get(buffer, dataItems.length), dataItems.length);
		assertEquals(buffer.size(), 2 * dataItems.length);
	}
	//
	@Test
	public void shouldClose()
	throws Exception {
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsSrc);
		itemsInput.close();
		Mockito.verifyNoMoreInteractions(itemsSrc);
	}
}
