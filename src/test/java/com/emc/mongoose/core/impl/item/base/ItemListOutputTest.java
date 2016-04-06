package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.data.DataItem;
//
//
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
//
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//
@RunWith(MockitoJUnitRunner.class)
public class ItemListOutputTest {
	//
	@Mock private List<DataItem> itemsMock;
	//
	@Test
	public void shouldWriteSingleItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		final ItemListOutput<DataItem> itemsOutput = new ItemListOutput<>(itemsMock);
		Mockito
			.when(itemsMock.add(dataItem))
			.thenReturn(true);
		itemsOutput.put(dataItem);
		Mockito.verify(itemsMock).add(dataItem);
	}
	//
	@Test
	public void shouldWriteManyItemsFromBuffer()
	throws Exception {
		final DataItem
			dataItems[] = new DataItem[] {
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class),
				Mockito.mock(DataItem.class)
			};
		final List<DataItem> buffer = Arrays.asList(dataItems);
		final List<DataItem> itemsDst = new ArrayList<>();
		final ItemListOutput<DataItem> itemsOutput = new ItemListOutput<>(itemsDst);
		Assert.assertEquals(itemsOutput.put(buffer), dataItems.length);
		Assert.assertEquals(itemsDst.size(), dataItems.length);
	}
	//
	@Test
	public void shouldClose()
	throws Exception {
		final ListItemSrc<DataItem> itemsInput = new ListItemSrc<>(itemsMock);
		itemsInput.close();
		Mockito.verifyNoMoreInteractions(itemsMock);
	}
}
