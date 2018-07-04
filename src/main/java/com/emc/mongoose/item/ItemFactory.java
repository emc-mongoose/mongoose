package com.emc.mongoose.item;

import java.io.Serializable;

/**
 Created by kurila on 14.07.16.
 */
public interface ItemFactory<I extends Item>
extends Serializable {
	
	I getItem(final String name, final long id, final long size)
	throws IllegalArgumentException;
	
	I getItem(final String line)
	throws IllegalArgumentException;

	Class<I> getItemClass();
}
