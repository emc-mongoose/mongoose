package com.emc.mongoose.core.api.data.src;
import com.emc.mongoose.core.api.data.DataItem;

import java.io.Externalizable;
import java.io.IOException;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface DataSource<T extends DataItem>
extends Externalizable {
	//
	int getSize();
	void setSize(final int size);
	//
	long getSeed();
	void setSeed(final long seed);
	//
	void fromString(final String v)
	throws IOException;
	//
}
