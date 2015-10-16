package com.emc.mongoose.core.impl.data.model;

import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
import com.emc.mongoose.core.impl.data.BasicObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.BufferedReader;
import java.lang.reflect.Constructor;

/**
 * Created by kirill_gusakov on 06.09.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class CSVItemSrcTest {

	@Mock
	private BufferedReader itemSrc;

	@Test
	public void shouldSkipSomeDataItems()
	throws Exception {
		final Constructor<BasicObject>
			itemConstructor = BasicObject.class.getConstructor(String.class, ContentSource.class);

		final CSVItemSrc<BasicObject>
			itemInput = new CSVItemSrc<>(itemSrc, itemConstructor);

		Mockito.mock(DataItem.class);
	}
}
