package com.emc.mongoose.model.io.task;

import com.emc.mongoose.common.supply.BatchSupplier;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.io.IoType;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 Created by kurila on 14.07.16.
 */
public interface IoTaskBuilder<I extends Item, O extends IoTask<I>>
extends Closeable {
	
	int getOriginCode();
	
	IoType getIoType();

	IoTaskBuilder<I, O> setIoType(final IoType ioType);

	String getInputPath();

	IoTaskBuilder<I, O> setInputPath(final String inputPath);
	
	IoTaskBuilder<I, O> setOutputPathSupplier(final BatchSupplier<String> ops);
	
	IoTaskBuilder<I, O> setUidSupplier(final BatchSupplier<String> uidSupplier);
	
	IoTaskBuilder<I, O> setSecretSupplier(final BatchSupplier<String> secretSupplier);
	
	IoTaskBuilder<I, O> setCredentialsMap(final Map<String, String> credentials);

	O getInstance(final I item)
	throws IOException, IllegalArgumentException;

	void getInstances(final List<I> items, final List<O> buff)
	throws IOException, IllegalArgumentException;
}
