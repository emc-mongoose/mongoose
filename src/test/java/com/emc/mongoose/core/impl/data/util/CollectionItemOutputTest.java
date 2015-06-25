package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import org.junit.Test;
import org.junit.runner.RunWith;
//
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.util.Collection;
//
@RunWith(MockitoJUnitRunner.class)
public class CollectionItemOutputTest {
	//
	@Mock private Collection<DataItem> itemsDst;
	//
	@Test
	public void shouldWriteSingleItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		final CollectionItemOutput<DataItem> itemsOutput = new CollectionItemOutput<>(itemsDst);
		itemsOutput.write(dataItem);
		Mockito.verify(itemsDst).add(dataItem);
	}
}
