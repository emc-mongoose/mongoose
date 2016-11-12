package com.emc.mongoose.model.item;

import java.io.Closeable;

/**
 Created by kurila on 14.07.16.
 */
public interface ItemFactory<I extends Item>
extends Closeable {
	
	I getItem(final String name, final long id, final long size);
	
	I getItem(final String line);
	
	Class<I> getItemClass();
}
