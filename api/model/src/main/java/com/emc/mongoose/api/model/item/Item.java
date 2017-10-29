package com.emc.mongoose.api.model.item;

import java.io.Externalizable;

/**
 Created by kurila on 11.07.16.
 */
public interface Item
extends Externalizable {

	String getName();

	void setName(final String name);

	void reset();

	String toString(final String itemPath);
}
