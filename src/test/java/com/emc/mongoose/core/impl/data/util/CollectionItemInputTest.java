package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import org.junit.Assert;
import org.junit.Test;
//
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.util.Collection;
import java.util.Iterator;
//
@RunWith(MockitoJUnitRunner.class)
public class CollectionItemInputTest {
	//
	@Mock private Collection<DataItem> itemsSrc;
	@Mock private Iterator<DataItem> itemsIter;
	//
	@Test
	public void shouldReadSingleItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		Mockito.when(itemsSrc.iterator())
			.thenReturn(itemsIter);
		Mockito.when(itemsIter.hasNext())
			.thenReturn(true)
			.thenReturn(false);
		Mockito.when(itemsIter.next())
			.thenReturn(dataItem)
			.thenReturn(null);
		//
		final CollectionItemInput<DataItem> itemsInput = new CollectionItemInput<>(itemsSrc);
		Assert.assertEquals(itemsInput.read(), dataItem);
	}
	//
	@Test
	public void shouldReturnNullWhenEndReached()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		Mockito.when(itemsSrc.iterator())
			.thenReturn(itemsIter);
		Mockito.when(itemsIter.hasNext())
			.thenReturn(true)
			.thenReturn(false);
		Mockito.when(itemsIter.next())
			.thenReturn(dataItem)
			.thenReturn(null);
		//
		final CollectionItemInput<DataItem> itemsInput = new CollectionItemInput<>(itemsSrc);
		itemsInput.read();
		Assert.assertEquals(null, itemsInput.read());
	}
	//
	@Test
	public void shouldReadFirstAfterReset()
	throws Exception {
		final DataItem
			dataItem1 = Mockito.mock(DataItem.class),
			dataItem2 = Mockito.mock(DataItem.class),
			dataItem3 = Mockito.mock(DataItem.class);
		final Iterator<DataItem>
			itemsIterNew = Mockito.mock(Iterator.class);
		Mockito.when(itemsSrc.iterator())
			.thenReturn(itemsIter)
			.thenReturn(itemsIterNew);
		Mockito.when(itemsIter.hasNext())
			.thenReturn(true)
			.thenReturn(true)
			.thenReturn(true)
			.thenReturn(false);
		Mockito.when(itemsIterNew.hasNext())
			.thenReturn(true)
			.thenReturn(true)
			.thenReturn(true)
			.thenReturn(false);
		Mockito.when(itemsIter.next())
			.thenReturn(dataItem1)
			.thenReturn(dataItem2)
			.thenReturn(dataItem3)
			.thenReturn(null);
		Mockito.when(itemsIterNew.next())
			.thenReturn(dataItem1)
			.thenReturn(dataItem2)
			.thenReturn(dataItem3)
			.thenReturn(null);
		//
		final CollectionItemInput<DataItem> itemsInput = new CollectionItemInput<>(itemsSrc);
		Assert.assertEquals(dataItem1, itemsInput.read());
		Assert.assertEquals(dataItem2, itemsInput.read());
		itemsInput.reset();
		Assert.assertEquals(dataItem1, itemsInput.read());
	}
	//
	@Test
	public void shouldClose()
	throws Exception {
		final CollectionItemInput<DataItem> itemsInput = new CollectionItemInput<>(itemsSrc);
		itemsInput.close();
		Mockito.verify(itemsSrc).clear();
	}
}
