package com.emc.mongoose.core.impl.data.model;

import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.impl.data.BasicObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.junit.Assert;

import java.io.BufferedReader;
import java.lang.reflect.Constructor;

/**
 * Created by kirill_gusakov on 06.09.15.
 */
@RunWith(MockitoJUnitRunner.class)
public class CSVItemInputTest {

	@Mock
	private BufferedReader itemSrc;

	@Test
	public void shouldSkipSomeDataItems()
	throws Exception {
		final Constructor<BasicObject> itemConstructor
			= BasicObject.class.getConstructor(String.class);

		final CSVItemInput<BasicObject> itemInput
			= new CSVItemInput<>(itemSrc, itemConstructor);

		Mockito.mock(DataItem.class);
	}
}