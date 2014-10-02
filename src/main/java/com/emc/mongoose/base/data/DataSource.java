package com.emc.mongoose.base.data;
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
	//
	void fromString(final String v)
	throws IOException;
	//
}
