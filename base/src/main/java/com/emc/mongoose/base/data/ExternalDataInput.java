package com.emc.mongoose.base.data;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.ReadableByteChannel;

/** Created by andrey on 24.07.17. */
public final class ExternalDataInput extends CachedDataInput {

	public ExternalDataInput() {
		super();
	}

	public ExternalDataInput(
					final ReadableByteChannel initialLayerInputChannel,
					final int layerSize,
					final int layersCacheCountLimit)
					throws IOException {
		super((MappedByteBuffer) ByteBuffer.allocateDirect(layerSize), layersCacheCountLimit);

		int doneByteCount = 0, n;

		// read the data from the channel to the inputBuff which is already initialized with the
		// parent constructor invocation
		while (doneByteCount < layerSize) {
			n = initialLayerInputChannel.read(inputBuff);
			if (n > 0) {
				doneByteCount += n;
			} else {
				break;
			}
		}

		// if there's not enough data read from the given input, repeat it
		final int inputSize = doneByteCount;
		final ByteBuffer initialData = inputBuff.asReadOnlyBuffer();
		while (doneByteCount < layerSize) {
			n = Math.min(inputBuff.remaining(), inputSize);
			initialData.position(0).limit(n);
			inputBuff.put(initialData);
			doneByteCount += n;
		}

		inputBuff.flip();
	}
}
