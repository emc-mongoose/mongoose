package com.emc.mongoose.core.impl.data.util;
//
import com.emc.mongoose.core.api.data.DataItem;
//
import com.emc.mongoose.core.api.data.util.DataItemInput;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 The data item input using CSV file containing the human-readable data item records as the source
 */
public class CSVFileItemInput<T extends DataItem>
implements DataItemInput<T> {
	//
	protected final Path itemsSrcPath;
	protected final BufferedReader itemsSrc;
	protected final Constructor<T> itemConstructor;
	/**
	 @param itemsSrcPath the path to the CSV file containing the data item records
	 @param itemCls the particular data item implementation class used to parse the records
	 @throws IOException
	 @throws NoSuchMethodException
	 */
	public CSVFileItemInput(final Path itemsSrcPath, final Class<T> itemCls)
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
