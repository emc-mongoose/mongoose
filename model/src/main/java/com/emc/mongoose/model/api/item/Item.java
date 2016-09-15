package com.emc.mongoose.model.api.item;

import java.io.Serializable;

/**
 Created by kurila on 11.07.16.
 */
public interface Item
extends Serializable {

	String SLASH = "/";

	String getPath();
	void setPath(final String path);

	String getName();
	void setName(final String name);
	
	void reset();
}
