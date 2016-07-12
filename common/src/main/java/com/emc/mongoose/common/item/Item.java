package com.emc.mongoose.common.item;

/**
 Created by kurila on 11.07.16.
 */
public interface Item {

	String SLASH = "/";

	String getPath();
	void setPath(final String path);

	String getName();
	void setName(final String name);
}
