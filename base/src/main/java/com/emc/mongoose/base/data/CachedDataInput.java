package com.emc.mongoose.base.data;

import static com.emc.mongoose.base.data.DataInput.generateData;
import static com.github.akurilov.commons.math.MathUtil.xorShift;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
* Created by andrey on 24.07.17. The data input able to produce the layer of different data using
* the given layer index. Also caches the layers using the layers count limit to not to exhaust the
* available memory. The allocated off-heap memory is calculated as layersCacheCountLimit *
* layerSize (worst case)
*/
public class CachedDataInput extends DataInputBase {

	private int layersCacheCountLimit;
	@SuppressWarnings("ThreadLocalNotStaticFinal")
	private final ThreadLocal<Int2ObjectOpenHashMap<MappedByteBuffer>> thrLocLayersCache = new ThreadLocal<>();

	public CachedDataInput() {
		super();
	}

	public CachedDataInput(final MappedByteBuffer initialLayer, final int layersCacheCountLimit) {
		super(initialLayer);
		if (layersCacheCountLimit < 1) {
			throw new IllegalArgumentException("Cache limit value should be more than 1");
		}
		this.layersCacheCountLimit = layersCacheCountLimit;
	}

	public CachedDataInput(final CachedDataInput other) {
		super(other);
		this.layersCacheCountLimit = other.layersCacheCountLimit;
	}

	private long getInitialSeed() {
		return inputBuff.getLong(0);
	}

	@Override
	public final MappedByteBuffer getLayer(final int layerIndex) throws OutOfMemoryError {

		if (layerIndex == 0) {
			return inputBuff;
		}
		var layersCache = thrLocLayersCache.get();
		if (layersCache == null) {
			layersCache = new Int2ObjectOpenHashMap<>(layersCacheCountLimit - 1);
			thrLocLayersCache.set(layersCache);
		}

		// check if layer exists
		var layer = layersCache.get(layerIndex - 1);
		if (layer == null) {
			// check if it's necessary to free the space first
			var layersCountToFree = layersCacheCountLimit - layersCache.size() + 1;
			final var layerSize = inputBuff.capacity();
			if (layersCountToFree > 0) {
				for (final int i : layersCache.keySet()) {
					layer = layersCache.remove(i);
					if (layer != null) {
						layersCountToFree--;
						if (layersCountToFree == 0) {
							break;
						}
					}
				}
				layersCache.trim();
			}
			// generate the layer
			layer = (MappedByteBuffer) ByteBuffer.allocateDirect(layerSize);
			final var layerSeed = Long.reverseBytes((xorShift(getInitialSeed()) << layerIndex) ^ layerIndex);
			generateData(layer, layerSeed);
			layersCache.put(layerIndex - 1, layer);
		}
		return layer;
	}

	public void close() throws IOException {
		super.close();
		final var layersCache = (Int2ObjectMap<MappedByteBuffer>) thrLocLayersCache.get();
		if (layersCache != null) {
			layersCache.clear();
			thrLocLayersCache.set(null);
		}
	}

	@Override
	public final String toString() {
		return Long.toHexString(getInitialSeed()) + ',' + Integer.toHexString(inputBuff.capacity());
	}
}
