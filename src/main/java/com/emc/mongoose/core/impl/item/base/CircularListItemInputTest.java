package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.core.api.item.data.DataItem;
//
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
//
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.util.Arrays;
import java.util.List;
//
@RunWith(MockitoJUnitRunner.class)
public class CircularListItemInputTest {
	//
	final DataItem
		dataItem0 = Mockito.mock(DataItem.class),
		dataItem1 = Mockito.mock(DataItem.class);
	final List<DataItem> items = Arrays.asList(new DataItem[] { dataItem0, dataItem1 });
	//
	@Test
	public void shouldReadMoreItemsThanStored()
	throws Exception {
		final Input<DataItem> itemInput = new CircularListItemInput<>(items);
		assertEquals(itemInput.get(), dataItem0);
		assertEquals(itemInput.get(), dataItem1);
		assertEquals(itemInput.get(), dataItem0);
		assertEquals(itemInput.get(), dataItem1);
		assertEquals(itemInput.get(), dataItem0);
	}
}
