package com.emc.mongoose.base.item;

import java.io.Externalizable;

/** Created by kurila on 11.07.16. */
public interface Item extends Externalizable {

	String name();

	void name(final String name);

	void reset();

	String toString(final String itemPath);
}
