package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.util.client.api.DataItemInput;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 Created by kurila on 18.06.15.
 */
public class TxtFileItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final Path itemsSrcPath;
	protected final BufferedReader itemsSrc;
	protected final Constructor<T> itemConstructor;
	//
	public TxtFileItemInput(final Path itemsSrcPath, final Class<T> itemCls)
	throws IOException, NoSuchMethodException {
		this.itemsSrcPath = itemsSrcPath;
		itemsSrc = Files.newBufferedReader(itemsSrcPath, StandardCharsets.UTF_8);
		itemConstructor = itemCls.getConstructor(String.class);
	}
	//
	@Override
	public T read()
	throws IOException {
		final String nextLine = itemsSrc.readLine();
		T nextItem = null;
		if(nextLine != null) {
			try {
				nextItem = itemConstructor.newInstance(nextLine);
			} catch(
				final InstantiationException | IllegalAccessException | InvocationTargetException e
			) {
				throw new IOException(e);
			}
		}
		return nextItem;
	}
	//
	@Override
	public void reset()
	throws IOException {
		itemsSrc.reset();
	}
	//
	@Override
	public void close()
	throws IOException {
		itemsSrc.close();
	}
}
