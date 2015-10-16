package com.emc.mongoose.core.api.data.content;
//
import java.io.Serializable;
import java.nio.ByteBuffer;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface ContentSource
extends Serializable {
	//
	int A = 21, B = 35, C = 4;
	//
	int getSize();
	//
	ByteBuffer getLayer(final int layerIndex);
}
