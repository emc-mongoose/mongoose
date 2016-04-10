package com.emc.mongoose.core.api.v1.item.base;
import java.io.Externalizable;
/**
 Created by kurila on 20.10.15.
 */
public interface Item
extends Externalizable {
	String getName();
	void setName(final String name);
}
