package com.emc.mongoose.model.api.data;

import java.io.Closeable;
import java.nio.ByteBuffer;
/**
 Created by kurila on 29.09.14.
 A finite data source for data generation purposes.
 */
public interface ContentSource
extends Cloneable, Closeable {

	enum Type {
		FILE, SEED
	}
	//
	int getSize();
	//
	ByteBuffer getLayer(final int layerIndex);
}
