package com.emc.mongoose.model.api.item;

import java.io.Externalizable;

/**
 Created by kurila on 11.07.16.
 */
public interface Item
extends Externalizable {

	String getName();

	void setName(final String name);

	void reset();
}
