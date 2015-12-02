package com.emc.mongoose.core.api.data.content;
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
	int A = 21, B = 35, C = 4, WORD_SIZE = Long.SIZE / Byte.SIZE;
	//
	int getSize();
	//
	ByteBuffer getLayer(final int layerIndex);
}
