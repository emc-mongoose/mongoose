package com.emc.mongoose.model.api.item;

/**
 Created by kurila on 14.07.16.
 */
public interface ItemFactory<I extends Item> {
	
	I getItem(final String path, final String name, final long id, final long size);
	
	I getItem(final String line);
	
	Class<I> getItemClass();
}
