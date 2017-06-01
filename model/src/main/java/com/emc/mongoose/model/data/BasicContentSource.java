package com.emc.mongoose.model.data;

import com.emc.mongoose.common.math.MathUtil;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 Created by kurila on 16.10.15.
 */
public class BasicContentSource
implements ContentSource {
	//
	protected long seed;
	private int cacheLimit;
	protected ByteBuffer zeroByteLayer;
	private transient final ThreadLocal<Int2ObjectOpenHashMap<ByteBuffer>> threadLocalByteLayersCache;
	//
	public BasicContentSource() {
		threadLocalByteLayersCache = new ThreadLocal<>();
	}
	//
	public BasicContentSource(final ByteBuffer zeroByteLayer, final int cacheLimit) {
		if(cacheLimit < 1) {
			throw new IllegalArgumentException("Cache limit value should be more than 1");
		}
		this.zeroByteLayer = zeroByteLayer;
		this.cacheLimit = cacheLimit;
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		zeroByteLayer.clear();
		threadLocalByteLayersCache = new ThreadLocal<>();
	}
	//
	protected BasicContentSource(
		final ReadableByteChannel zeroLayerSrcChan, final int size, final int cacheLimit
	) throws IOException {
		zeroByteLayer = ByteBuffer.allocate(size);
		int n = 0, m;
		do {
			m = zeroLayerSrcChan.read(zeroByteLayer);
			if(m < 0) {
				break;
			} else {
				n += m;
			}
		} while(n < size);
		zeroByteLayer.flip();
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		this.cacheLimit = cacheLimit;
		threadLocalByteLayersCache = new ThreadLocal<>();
	}
	//
	protected BasicContentSource(final BasicContentSource other) {
		this.seed = other.seed;
		this.cacheLimit = other.cacheLimit;
		this.zeroByteLayer = other.zeroByteLayer;
		this.threadLocalByteLayersCache = new ThreadLocal<>();
	}
	//
	@Override
	public final int getSize() {
		// NPE protection is necessary for the storage driver service
		return zeroByteLayer == null ? 0 : zeroByteLayer.capacity();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final ByteBuffer getLayer(final int layerIndex) {
		//
		if(layerIndex == 0) {
			return zeroByteLayer;
		}
		//
		Int2ObjectOpenHashMap<ByteBuffer> byteLayersCache = threadLocalByteLayersCache.get();
		if(byteLayersCache == null) {
			byteLayersCache = new Int2ObjectOpenHashMap<>(cacheLimit);
			threadLocalByteLayersCache.set(byteLayersCache);
		}
		// check if layer exists
		ByteBuffer layer = byteLayersCache.get(layerIndex);
		if(layer == null) {
			// else generate
			final int size = zeroByteLayer.capacity();
			layer = ByteBuffer.allocateDirect(size);
			final long layerSeed = Long.reverseBytes((seed << layerIndex) ^ layerIndex);
			generateData(layer, layerSeed);
			byteLayersCache.put(layerIndex, layer);
			if(byteLayersCache.size() > cacheLimit) {
				// do not remove the zero byte layer, start from the layer #1
				for(int i = 0; i < layerIndex; i ++) {
					if(null != byteLayersCache.remove(i)) {
						// stop if some lowest index layer was removed
						break;
					}
				}
				byteLayersCache.trim();
			}
		}
		return layer;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	protected static void generateData(final ByteBuffer byteLayer, final long seed) {
		final int
			ringBuffSize = byteLayer.capacity(),
			countWordBytes = Long.SIZE / Byte.SIZE,
			countWords = ringBuffSize / countWordBytes,
			countTailBytes = ringBuffSize % countWordBytes;
		long word = seed;
		int i;
		// 64-bit words
		byteLayer.clear();
		for(i = 0; i < countWords; i ++) {
			byteLayer.putLong(word);
			word = MathUtil.xorShift(word);
		}
		// tail bytes
		final ByteBuffer tailBytes = ByteBuffer.allocateDirect(countWordBytes);
		tailBytes.asLongBuffer().put(word).rewind();
		for(i = 0; i < countTailBytes; i ++) {
			byteLayer.put(countWordBytes * countWords + i, tailBytes.get(i));
		}
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeLong(seed);
		out.writeInt(cacheLimit);
		// write buffer capacity and data
		final byte buff[] = new byte[zeroByteLayer.capacity()];
		zeroByteLayer.clear(); // reset the position
		zeroByteLayer.get(buff);
		out.writeInt(buff.length);
		out.write(buff);
	}

	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		seed = in.readLong();
		cacheLimit = in.readInt();
		//read buffer data and wrap with ByteBuffer
		final int size = in.readInt();
		final byte buff[] = new byte[size];
		for(int i, j = 0; j < size;) {
			i = in.read(buff, j, size - j);
			if(i == -1) {
				break;
			} else {
				j += i;
			}
		}
		zeroByteLayer = ByteBuffer.allocateDirect(size).put(buff);
	}

	//
	@Override
	public final void close()
	throws IOException {
		zeroByteLayer = null;
	}
}
