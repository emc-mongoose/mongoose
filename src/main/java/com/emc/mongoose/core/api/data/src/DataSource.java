package com.emc.mongoose.core.api.data.src;
//
import java.io.Externalizable;
import java.io.IOException;
import java.nio.ByteBuffer;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface DataSource
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
	ByteBuffer getLayer(final int layerIndex);
}
