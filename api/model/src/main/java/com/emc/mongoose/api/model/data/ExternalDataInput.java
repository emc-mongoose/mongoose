package com.emc.mongoose.api.model.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 Created by andrey on 24.07.17.
 */
public final class ExternalDataInput
extends CachedDataInput {

	public ExternalDataInput() {
		super();
	}

	public ExternalDataInput(
		final ReadableByteChannel initialLayerInputChannel, final int layerSize,
		final int layersCacheCountLimit
	) throws IOException {
		super((MappedByteBuffer) ByteBuffer.allocateDirect(layerSize), layersCacheCountLimit);

		int readByteCount = 0, m;

		// read the data from the channel to the inputBuff which is already initialized with the
		// parent constructor invocation
		while(readByteCount < layerSize) {
			m = initialLayerInputChannel.read(inputBuff);
			if(m > 0) {
				readByteCount += m;
			} else {
				break;
			}
		}

		// if there's not enough data read from the given input, repeat it
		final int inputSize = readByteCount;
		final ByteBuffer initialData = inputBuff.asReadOnlyBuffer();
		while(readByteCount < layerSize) {
			initialData.position(0).limit(Math.min(inputBuff.remaining(), inputSize));
			inputBuff.put(initialData);
		}

		inputBuff.flip();
	}
}
