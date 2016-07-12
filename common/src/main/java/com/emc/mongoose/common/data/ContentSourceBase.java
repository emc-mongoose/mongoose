package com.emc.mongoose.common.data;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.math.MathUtil;
import com.emc.mongoose.common.util.SizeInBytes;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Map;
/**
 Created by kurila on 16.10.15.
 */
public abstract class ContentSourceBase
implements ContentSource {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected ByteBuffer zeroByteLayer = null;
	protected long seed = 0;
	//
	protected transient Map<Integer, ByteBuffer> byteLayersMap = null;
	//
	protected ContentSourceBase() {
	}
	//
	protected ContentSourceBase(final ByteBuffer zeroByteLayer) {
		this.zeroByteLayer = zeroByteLayer;
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		zeroByteLayer.clear();
		//
		byteLayersMap = new LRUMap<>(
			(int) SizeInBytes.toFixedSize("100MB") / zeroByteLayer.capacity()
		);
		byteLayersMap.put(0, zeroByteLayer);
	}
	//
	protected ContentSourceBase(final ReadableByteChannel zeroLayerSrcChan, final int size)
	throws IOException {
		this.zeroByteLayer = ByteBuffer.allocateDirect(size);
		this.seed = MathUtil.xorShift(zeroByteLayer.getLong());
		zeroByteLayer.clear();
		byteLayersMap = new LRUMap<>(
			(int) SizeInBytes.toFixedSize("100MB") / zeroByteLayer.capacity()
		);
		byteLayersMap.put(0, zeroByteLayer);
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
	protected ContentSourceBase(final ContentSourceBase anotherContentSource) {
		this.zeroByteLayer = anotherContentSource.zeroByteLayer;
		this.seed = anotherContentSource.seed;
		byteLayersMap = new LRUMap<>(
			(int) SizeInBytes.toFixedSize("100MB") / zeroByteLayer.capacity()
		);
		byteLayersMap.put(0, zeroByteLayer);
	}
	//
	@Override
	public final int getSize() {
		return zeroByteLayer.capacity();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeLong(seed);
		final byte buff[] = new byte[zeroByteLayer.capacity()];
		zeroByteLayer.clear(); // reset
		zeroByteLayer.get(buff);
		out.writeInt(buff.length);
		out.write(buff);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		seed = in.readLong();
		int size = in.readInt();
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
		byteLayersMap = new LRUMap<>(
			(int) SizeInBytes.toFixedSize("100MB") / zeroByteLayer.capacity()
		);
		byteLayersMap.put(0, zeroByteLayer);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final ByteBuffer getLayer(final int layerIndex) {
		// zero layer always exists so it may be useful to do it very simply and fast
		if(layerIndex == 0) {
			return zeroByteLayer;
		}
		// else
		long nextSeed;
		final int size = zeroByteLayer.capacity();
		final ByteBuffer layer;
		synchronized(byteLayersMap) {
			if(byteLayersMap.containsKey(layerIndex)) {
				layer = byteLayersMap.get(layerIndex);
			} else {
				layer = ByteBuffer.allocateDirect(size);
				nextSeed = Long.reverseBytes((seed << layerIndex) ^ layerIndex);
				LOG.debug(
					Markers.MSG,
					"Generate new byte layer #{} using the seed: \"{}\"",
					layerIndex, Long.toHexString(nextSeed)
				);
				generateData(layer, nextSeed);
				byteLayersMap.put(layerIndex, layer);
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
		double d = System.nanoTime();
		LOG.debug(
			Markers.MSG, "Prepare {} of ring data...", SizeInBytes.formatFixedSize(ringBuffSize)
		);
		// 64-bit words
		byteLayer.clear();
		for(i = 0; i < countWords; i ++) {
			byteLayer.putLong(word);
			word = MathUtil.xorShift(word);
		}
		// tail bytes\
		final ByteBuffer tailBytes = ByteBuffer.allocateDirect(countWordBytes);
		tailBytes.asLongBuffer().put(word).rewind();
		for(i = 0; i < countTailBytes; i ++) {
			byteLayer.put(countWordBytes * countWords + i, tailBytes.get(i));
		}
		/*if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(
				LogUtil.MSG, "Ring buffer data: {}", Base64.encodeBase64String(byteLayer.array())
			);
		}*/
		//
		LOG.debug(
			Markers.MSG, "Pre-generating the data done in {}[us]",
			(System.nanoTime() - d) / 1e9
		);
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(byteLayersMap != null) {
			byteLayersMap.clear();
			byteLayersMap = null;
		}
		zeroByteLayer = null;
	}
}
