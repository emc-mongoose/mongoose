package com.emc.mongoose.model.data;

import com.emc.mongoose.common.math.MathUtil;

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
	protected ByteBuffer zeroByteLayer;
	private transient ByteBuffer byteLayers[];
	protected long seed;
	private int cacheLimit;
	//
	public BasicContentSource() {
	}
	//
	public BasicContentSource(final ByteBuffer zeroByteLayer, final int cacheLimit) {
		this.zeroByteLayer = zeroByteLayer;
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		zeroByteLayer.clear();
		this.cacheLimit = cacheLimit;
		byteLayers = new ByteBuffer[cacheLimit];
		byteLayers[0] = zeroByteLayer;
	}
	//
	protected BasicContentSource(
		final ReadableByteChannel zeroLayerSrcChan, final int size, final int cacheLimit
	) throws IOException {
		this.zeroByteLayer = ByteBuffer.allocateDirect(size);
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		zeroByteLayer.clear();
		this.cacheLimit = cacheLimit;
		byteLayers = new ByteBuffer[cacheLimit];
		byteLayers[0] = zeroByteLayer;
		int n = 0, m;
		do {
			m = zeroLayerSrcChan.read(zeroByteLayer);
			if(m < 0) {
				break;
			} else {
				n += m;
			}
		} while(n < size);
	}
	//
	protected BasicContentSource(final BasicContentSource anotherContentSource) {
		this.zeroByteLayer = anotherContentSource.zeroByteLayer;
		this.seed = anotherContentSource.seed;
		this.cacheLimit = anotherContentSource.cacheLimit;
		byteLayers = new ByteBuffer[this.cacheLimit];
		byteLayers[0] = zeroByteLayer;
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
		// zero layer always exists so it may be useful to do it very simply and fast
		if(layerIndex == 0) {
			return zeroByteLayer;
		}
		// else fast check if layer exists
		ByteBuffer layer = byteLayers[layerIndex];
		if(layer == null) {
			// else generate
			long nextSeed;
			final int size = zeroByteLayer.capacity();
			layer = ByteBuffer.allocateDirect(size);
			nextSeed = Long.reverseBytes((seed << layerIndex) ^ layerIndex);
			System.out.println("Generate the data for the layer #" + layerIndex);
			generateData(layer, nextSeed);
			byteLayers[layerIndex] = layer;
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
		zeroByteLayer.clear(); // reset
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
		byteLayers = new ByteBuffer[cacheLimit];
		byteLayers[0] = zeroByteLayer;
	}

	//
	@Override
	public final void close()
	throws IOException {
		for(int i = 0; i < byteLayers.length; i ++) {
			byteLayers[i] = null;
		}
	}
}
