package com.emc.mongoose.core.impl.data.content;
// mongoose-common
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-api
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
/**
 Created by kurila on 23.07.14.
 A uniform data source for producing uniform data items.
 Implemented as finite buffer of pseudorandom bytes.
 */
@Deprecated
public final class UniformContentSource
extends ContentSourceBase
implements ContentSource {
	//
	private final static Logger LOG = LogManager.getLogger();
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final static long DEFAULT_SEED = Long.valueOf("7a42d9c483244167", 0x10);
	//
	private final long seed;
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Deprecated
	public UniformContentSource()
	throws NumberFormatException {
		this(
			Long.parseLong(RunTimeConfig.getContext().getDataSrcRingSeed(), 0x10),
			(int) RunTimeConfig.getContext().getDataSrcRingSize()
		);
	}
	//
	public UniformContentSource(final long seed, final int size) {
		super(ByteBuffer.allocateDirect(size));
		this.seed = seed;
		generateData(zeroByteLayer, seed);
		LOG.debug(Markers.MSG, "New ring buffer instance #{}", hashCode());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private static void generateData(final ByteBuffer byteLayer, final long seed) {
		final int
			ringBuffSize = byteLayer.capacity(),
			countWordBytes = Long.SIZE / Byte.SIZE,
			countWords = ringBuffSize / countWordBytes,
			countTailBytes = ringBuffSize % countWordBytes;
		long word = seed;
		int i;
		double d = System.nanoTime();
		LOG.debug(Markers.MSG, "Prepare {} of ring data...", SizeUtil.formatSize(ringBuffSize));
		// 64-bit words
		byteLayer.clear();
		for(i = 0; i < countWords; i ++) {
			byteLayer.putLong(word);
			word = nextWord(word);
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
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int getSize() {
		return size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		return
			Long.toHexString(seed) + RunTimeConfig.LIST_SEP +
			Integer.toHexString(zeroByteLayer.capacity());
	}
	//
	public static UniformContentSource fromString(final String metaInfo)
		throws IllegalArgumentException, IOException {
		final String values[] = metaInfo.split(RunTimeConfig.LIST_SEP);
		if(values.length == 2) {
			return new UniformContentSource(
				Long.parseLong(values[0], 0x10), Integer.parseInt(values[1], 0x10)
			);
		} else {
			throw new IllegalArgumentException();
		}
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
		synchronized(byteLayers) {
			for (int i = byteLayers.size(); i <= layerIndex; i++) {
				ByteBuffer nextLayer = ByteBuffer.allocateDirect(size);
				nextSeed = Long.reverse(
					nextWord(
						Long.reverseBytes(
							byteLayers.get(i - 1).getLong(0)
						)
					)
				);
				LOG.debug(
					Markers.MSG,
					"Generate new byte layer #{} using the seed: \"{}\"",
					i, Long.toHexString(nextSeed)
				);
				generateData(nextLayer, nextSeed);
				byteLayers.add(nextLayer);
			}
		}
		return byteLayers.get(layerIndex);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
