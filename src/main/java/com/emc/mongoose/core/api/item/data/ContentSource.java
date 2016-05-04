package com.emc.mongoose.core.api.item.data;
//
import java.io.Externalizable;
import java.nio.ByteBuffer;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface ContentSource
extends Externalizable {
	//
	int getSize();
	//
	ByteBuffer getLayer(final int layerIndex);
}
