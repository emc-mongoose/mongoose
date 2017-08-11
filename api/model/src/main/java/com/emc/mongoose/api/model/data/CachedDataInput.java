package com.emc.mongoose.api.model.data;

import static com.emc.mongoose.api.common.math.MathUtil.xorShift;
import static com.emc.mongoose.api.model.data.DataInput.generateData;

import com.emc.mongoose.api.common.SizeInBytes;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.LongAdder;

import static java.nio.ByteBuffer.allocateDirect;

/**
 Created by andrey on 24.07.17.
 The data input able to produce the layer of different data using the given layer index.
 Also caches the layers using the layers count limit to not to exhaust the available memory.
 The allocated off-heap memory is calculated as layersCacheCountLimit * layerSize (worst case)
 */
public class CachedDataInput
extends DataInputBase {

	private static final LongAdder ALLOCATED_DIRECT_MEM = new LongAdder();

	private int layersCacheCountLimit;
	private transient final ThreadLocal<Int2ObjectOpenHashMap<ByteBuffer>>
		thrLocLayersCache = new ThreadLocal<>();

	public CachedDataInput() {
		super();
	}

	public CachedDataInput(final ByteBuffer initialLayer, final int layersCacheCountLimit) {
		super(initialLayer);
		if(layersCacheCountLimit < 1) {
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
	public final ByteBuffer getLayer(final int layerIndex)
	throws OutOfMemoryError {

		if(layerIndex == 0) {
			return inputBuff;
		}

		Int2ObjectOpenHashMap<ByteBuffer> layersCache = thrLocLayersCache.get();
		if(layersCache == null) {
			layersCache = new Int2ObjectOpenHashMap<>(layersCacheCountLimit - 1);
			thrLocLayersCache.set(layersCache);
		}

		// check if layer exists
		ByteBuffer layer = layersCache.get(layerIndex - 1);
		if(layer == null) {
			// check if it's necessary to free the space first
			if(layersCache.size() == layersCacheCountLimit - 1) {
				for(int i = 0; i < layerIndex - 1; i ++) {
					if(null != layersCache.remove(i)) {
						ALLOCATED_DIRECT_MEM.add(-inputBuff.capacity());
						// some lowest index layer was removed, stop the loop
						break;
					}
				}
				layersCache.trim();
			}
			// generate the layer
			final int size = inputBuff.capacity();
			try {
				layer = allocateDirect(size);
				ALLOCATED_DIRECT_MEM.add(size);
			} catch (final OutOfMemoryError e) {
				System.err.println(
					"Allocated direct memory: " +
						SizeInBytes.formatFixedSize(ALLOCATED_DIRECT_MEM.sum()) +
						", layers cache size: " + layersCache.size()
				);
				throw e;
			}
			final long layerSeed = Long.reverseBytes(
				(xorShift(getInitialSeed()) << layerIndex) ^ layerIndex
			);
			generateData(layer, layerSeed);
			layersCache.put(layerIndex - 1, layer);
		}
		return layer;
	}

	public void close()
	throws IOException {
		final Int2ObjectOpenHashMap<ByteBuffer> layersCache = thrLocLayersCache.get();
		if(layersCache != null) {
			for(final ByteBuffer layer : layersCache.values()) {
				if(layer != null) {
					ALLOCATED_DIRECT_MEM.add(-layer.capacity());
				}
			}
			layersCache.clear();
			thrLocLayersCache.set(null);
		}
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(layersCacheCountLimit);
	}

	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		layersCacheCountLimit = in.readInt();
	}

	@Override
	public final String toString() {
		return Long.toHexString(getInitialSeed()) + ',' + Integer.toHexString(inputBuff.capacity());
	}
}
