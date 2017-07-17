package com.emc.mongoose.api.model.data;

import java.nio.ByteBuffer;
/**
 Created by kurila on 23.07.14.
 A uniform data source for producing uniform data items.
 Implemented as finite buffer of pseudorandom bytes.
 */
public final class SeedContentSource
extends BasicContentSource {

	public SeedContentSource() {
		super();
	}
	//
	public SeedContentSource(final long seed, final long size, final int cacheLimit) {
		super(ByteBuffer.allocateDirect((int) size), cacheLimit);
		this.seed = seed;
		generateData(zeroByteLayer, seed);
	}
	//
	public SeedContentSource(final SeedContentSource anotherContentSource) {
		super(anotherContentSource);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		return Long.toHexString(seed) + ',' + Integer.toHexString(zeroByteLayer.capacity());
	}
}
