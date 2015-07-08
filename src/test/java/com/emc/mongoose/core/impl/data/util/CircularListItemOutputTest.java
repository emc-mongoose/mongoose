package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.model.DataItemOutput;
import com.emc.mongoose.core.impl.data.model.CircularListItemOutput;
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
		final DataItemOutput<DataItem> itemOutput = new CircularListItemOutput<>(itemDst, 2);
		itemOutput.write(dataItem0);
		assertEquals(itemDst.get(0), dataItem0);
		itemOutput.write(dataItem1);
		assertEquals(itemDst.get(1), dataItem1);
		itemOutput.write(dataItem2);
		assertEquals(itemDst.get(0), dataItem2);
		itemOutput.write(dataItem3);
		assertEquals(itemDst.get(1), dataItem3);
		itemOutput.write(dataItem4);
		assertEquals(itemDst.get(0), dataItem4);
	}
	//
	@Test
	public void shouldWriteItemsCircularilyBulk()
	throws Exception {
		final List<DataItem> itemDst = new ArrayList<>(2);
		final DataItemOutput<DataItem> itemOutput = new CircularListItemOutput<>(itemDst, 2);
		final List<DataItem> buffer = Arrays.asList(
			new DataItem[] {dataItem0, dataItem1, dataItem2, dataItem3, dataItem4}
		);
		assertEquals(buffer.size(), itemOutput.write(buffer));
		assertEquals(itemDst.get(0), dataItem3);
		assertEquals(itemDst.get(1), dataItem4);
	}
}
