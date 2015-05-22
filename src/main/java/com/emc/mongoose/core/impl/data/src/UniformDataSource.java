package com.emc.mongoose.core.impl.data.src;
// mongoose-common
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.logging.LogUtil;
// mongoose-core-api
import com.emc.mongoose.core.api.data.src.DataSource;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
/**
 Created by kurila on 23.07.14.
 A uniform data source for producing uniform data items.
 Implemented as finite buffer of pseudorandom bytes.
 */
public class UniformDataSource
implements DataSource {
	//
	private final static Logger LOG = LogManager.getLogger();
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static int A = 21, B = 35, C = 4;
	//
	private long seed;
	private int size;
	private final List<ByteBuffer> byteLayers = new ArrayList<>(1);
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformDataSource()
	throws NumberFormatException {
		this(
			Long.parseLong(RunTimeConfig.getContext().getDataBufferRingSeed(), 0x10),
			(int) RunTimeConfig.getContext().getDataBufferRingSize()
		);
	}
	//
	protected UniformDataSource(final long seed, final int size) {
		LOG.debug(LogUtil.MSG, "New ring buffer instance #{}", hashCode());
		this.seed = seed;
		this.size = size;
		final ByteBuffer zeroByteLayer = ByteBuffer.allocate(size);
		generateData(zeroByteLayer, seed);
		byteLayers.add(zeroByteLayer);
	}
	//
	public static UniformDataSource DEFAULT = null;
	static {
		try {
			DEFAULT = new UniformDataSource();
			//LOG.info(Markers.MSG, "Default data source: {}", DEFAULT.toString());
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to create default data source");
		}
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
		LOG.debug(LogUtil.MSG, "Prepare {} of ring data...", SizeUtil.formatSize(ringBuffSize));
		// 64-bit words
		byteLayer.clear();
		for(i = 0; i < countWords; i ++) {
			byteLayer.putLong(word);
			word = nextWord(word);
		}
		// tail bytes\
		final ByteBuffer tailBytes = ByteBuffer.allocate(countWordBytes);
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
			LogUtil.MSG, "Pre-generating the data done in {}[us]",
			(System.nanoTime() - d) / LoadExecutor.NANOSEC_SCALEDOWN
		);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// See for details: http://xorshift.di.unimi.it/murmurhash3.c //////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static long nextWord(long word) {
		word ^= (word << A);
		word ^= (word >>> B);
		word ^= (word << C);
		return word;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeInt(byteLayers.get(0).capacity());
		out.writeLong(seed);
	}
	//
	@Override
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		DEFAULT.setSize(in.readInt());
		DEFAULT.setSeed(in.readLong());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int getSize() {
		return size;
	}
	//
	@Override
	public final void setSize(final int size)
	throws IllegalArgumentException {
		if(size < 1) {
			throw new IllegalArgumentException("Illegal ring size: " + size);
		}
		this.size = size;
		final ByteBuffer zeroByteLayer = ByteBuffer.allocate(size);
		generateData(zeroByteLayer, seed);
		byteLayers.clear();
		byteLayers.add(zeroByteLayer);
	}
	//
	@Override
	public final long getSeed() {
		return seed;
	}
	//
	@Override
	public final void setSeed(final long seed) {
		this.seed = seed;
		final ByteBuffer zeroByteLayer = ByteBuffer.allocate(size);
		generateData(zeroByteLayer, seed);
		byteLayers.clear();
		byteLayers.add(zeroByteLayer);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		return
			Long.toHexString(seed) + RunTimeConfig.LIST_SEP +
			Integer.toHexString(byteLayers.get(0).capacity());
	}
	//
	@Override
	public void fromString(final String metaInfo)
		throws IllegalArgumentException, IOException {
		final String values[] = metaInfo.split(RunTimeConfig.LIST_SEP);
		if(values.length == 2) {
			DEFAULT = new UniformDataSource(
				Long.parseLong(values[0], 0x10), Integer.parseInt(values[1], 0x10)
			);
		} else {
			throw new IllegalArgumentException();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final synchronized ByteBuffer getLayer(final int layerIndex) {
		final int layerCount = byteLayers.size();
		if(layerIndex >= layerCount) {
			ByteBuffer prevLayer = byteLayers.get(layerCount - 1), nextLayer;
			long prevSeed, nextSeed;
			final int ringSize = prevLayer.capacity();
			for(int i = layerCount; i <= layerIndex; i ++) {
				nextLayer = ByteBuffer.allocate(ringSize);
				prevSeed = prevLayer.getLong(0);
				nextSeed = Long.reverse(nextWord(Long.reverseBytes(prevSeed)));
				LOG.debug(
					LogUtil.MSG,
					"Generate new byte layer #{}, previous seed: \"{}\", next one: \"{}\"",
					i, Long.toHexString(prevSeed), Long.toHexString(nextSeed)
				);
				generateData(nextLayer, nextSeed);
				byteLayers.add(nextLayer);
				prevLayer = nextLayer;
			}
			LOG.debug(LogUtil.MSG, "New layer #{}", byteLayers.size() - 1);
		}
		return byteLayers.get(layerIndex);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
