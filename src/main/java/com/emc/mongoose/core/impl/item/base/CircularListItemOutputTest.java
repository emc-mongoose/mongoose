package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.common.io.Output;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
//
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
//
@RunWith(MockitoJUnitRunner.class)
public class CircularListItemOutputTest {
	//
	final DataItem
		dataItem0 = Mockito.mock(DataItem.class),
		dataItem1 = Mockito.mock(DataItem.class),
		dataItem2 = Mockito.mock(DataItem.class),
		dataItem3 = Mockito.mock(DataItem.class),
		dataItem4 = Mockito.mock(DataItem.class);
	//
	@Test
	public void shouldWriteItemsCircularily()
	throws Exception {
		final List<DataItem> itemDst = new ArrayList<>(2);
		final Output<DataItem> itemOutput = new CircularListItemOutput<>(itemDst, 2);
		itemOutput.put(dataItem0);
		assertEquals(itemDst.get(0), dataItem0);
		itemOutput.put(dataItem1);
		assertEquals(itemDst.get(1), dataItem1);
		itemOutput.put(dataItem2);
		assertEquals(itemDst.get(0), dataItem2);
		itemOutput.put(dataItem3);
		assertEquals(itemDst.get(1), dataItem3);
		itemOutput.put(dataItem4);
		assertEquals(itemDst.get(0), dataItem4);
	}
	//
	@Test
	public void shouldWriteItemsCircularilyBulk()
	throws Exception {
		final List<DataItem> itemDst = new ArrayList<>(2);
		final Output<DataItem> itemOutput = new CircularListItemOutput<>(itemDst, 2);
		final List<DataItem> buffer = Arrays.asList(
			new DataItem[] {dataItem0, dataItem1, dataItem2, dataItem3, dataItem4}
		);
		assertEquals(buffer.size(), itemOutput.put(buffer));
		assertEquals(itemDst.get(0), dataItem3);
		assertEquals(itemDst.get(1), dataItem4);
	}
}
