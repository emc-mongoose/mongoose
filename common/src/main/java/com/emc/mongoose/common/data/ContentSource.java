package com.emc.mongoose.common.data;

import java.io.Closeable;
import java.io.Externalizable;
import java.nio.ByteBuffer;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface ContentSource
extends Cloneable, Closeable, Externalizable {
	//
	int getSize();
	//
	ByteBuffer getLayer(final int layerIndex);
}
