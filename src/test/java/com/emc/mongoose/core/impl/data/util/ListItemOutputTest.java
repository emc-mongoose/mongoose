package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.impl.data.model.ListItemInput;
import com.emc.mongoose.core.impl.data.model.ListItemOutput;
import org.junit.Test;
import org.junit.runner.RunWith;
//
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
//
import java.io.IOException;
import java.util.List;
//
@RunWith(MockitoJUnitRunner.class)
public class ListItemOutputTest {
	//
	@Mock private List<DataItem> itemsMock;
	@Mock private ListItemInput<DataItem> itemsInputMock;
	//
	@Test
	public void shouldWriteSingleItem()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		final ListItemOutput<DataItem> itemsOutput = new ListItemOutput<>(itemsMock);
		Mockito
			.when(itemsMock.add(dataItem))
			.thenReturn(true);
		itemsOutput.write(dataItem);
		Mockito.verify(itemsMock).add(dataItem);
	}
	//
	@Test(expected = IOException.class)
	public void shouldThrowIOExceptionWhenOutOfCapacity()
	throws Exception {
		final DataItem dataItem = Mockito.mock(DataItem.class);
		final ListItemOutput<DataItem> itemsOutput = new ListItemOutput<>(itemsMock);
		Mockito
			.when(itemsMock.add(dataItem))
			.thenReturn(false);
		itemsOutput.write(dataItem);
	}
	//
	@Test
	public void shouldClose()
	throws Exception {
		final ListItemInput<DataItem> itemsInput = new ListItemInput<>(itemsMock);
		itemsInput.close();
		Mockito.verify(itemsMock).clear();
	}
}
