package com.emc.mongoose.base.data;

import static com.emc.mongoose.base.data.DataInput.generateData;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
* Created by kurila on 23.07.14. A uniform data input for producing uniform data items. Implemented
* as finite buffer of pseudo random bytes.
*/
public final class SeedDataInput extends CachedDataInput {

	public SeedDataInput() {
		super();
	}

	public SeedDataInput(final long seed, final int size, final int cacheLimit) {
		super((MappedByteBuffer) ByteBuffer.allocateDirect(size), cacheLimit);
		generateData(inputBuff, seed);
	}

	public SeedDataInput(final SeedDataInput other) {
		super(other);
	}
}
