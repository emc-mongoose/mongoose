package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.http.annotation.ThreadSafe;
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
@ThreadSafe
public class UniformDataSource<T extends UniformData>
implements DataSource<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static int A = 21, B = 35, C = 4;
	private final static String
		MSG_FMT_NEW_LAYER = "Generate new byte layer #%d, previous seed: \"%x\", next one: \"%x\"";
	//
	private long seed;
	private List<ByteBuffer> byteLayers = new ArrayList<>(1);
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformDataSource()
	throws NumberFormatException {
		this(
			Long.parseLong(Main.RUN_TIME_CONFIG.getString("data.ring.seed"), 0x10),
			(int) Main.RUN_TIME_CONFIG.getSizeBytes("data.ring.size")
		);
	}
	//
	protected UniformDataSource(final long seed, final int size) {
		this.seed = seed;
		final ByteBuffer zeroByteLayer = ByteBuffer.allocate(size);
		generateData(zeroByteLayer, seed);
		byteLayers.add(zeroByteLayer);
	}
	//
	public static UniformDataSource DEFAULT = null;
	static {
		try {
			DEFAULT = new UniformDataSource();
			LOG.info(Markers.MSG, "Default data source: {}", DEFAULT.toString());
		} catch(final Exception e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "Failed to create default data source");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	private static void generateData(final ByteBuffer byteLayer, final long seed) {
		//final LongBuffer wordLayerView = byteLayer.asLongBuffer();
		final int
			size = byteLayer.array().length,
			countWordBytes = Long.SIZE / Byte.SIZE,
			countWords = size / countWordBytes,
			countTailBytes = size % countWordBytes;
		long word = seed;
		int i;
		double d = System.nanoTime();
		LOG.debug(Markers.MSG, "Prepare {} of ring data...", RunTimeConfig.formatSize(size));
		// 64-bit words
		for(i = 0; i < countWords; i++) {
			byteLayer.putLong(word);
			//wordLayerView.putL(i, word);
			word = nextWord(word);
		}
		// tail bytes
		final ByteBuffer tailBytes = ByteBuffer.allocate(countWordBytes);
		tailBytes.asLongBuffer().put(word).rewind();
		for(i = 0; i < countTailBytes; i++) {
			byteLayer.put(countWordBytes * countWords + i, tailBytes.get(i));
		}
		//
		LOG.debug(
			Markers.MSG, "Pre-generating the data done in {} seconds",
			(System.nanoTime() - d) / LoadExecutor.BILLION
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
		out.writeInt(byteLayers.get(0).array().length);
		out.writeLong(seed);
	}
	//
	@Override
	public final void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
		final ByteBuffer ringZeroLayer = ByteBuffer.allocate(in.readInt());
		seed = in.readLong();
		generateData(ringZeroLayer, seed);
		byteLayers.clear();
		byteLayers.add(ringZeroLayer);
		DEFAULT = this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int getSize() {
		return byteLayers.get(0).array().length;
	}
	//
	public final long getSeed() {
		return seed;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String toString() {
		return
			Long.toHexString(seed) + RunTimeConfig.LIST_SEP +
			Integer.toHexString(byteLayers.get(0).array().length);
	}
	//
	@Override
	public void fromString(final String metaInfo)
		throws IllegalArgumentException, IOException {
		final String values[] = metaInfo.split(RunTimeConfig.LIST_SEP);
		if(values.length==2) {
			DEFAULT = new UniformDataSource(
				Long.parseLong(values[0], 0x10), Integer.parseInt(values[1], 0x10)
			);
		} else {
			throw new IllegalArgumentException();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final synchronized byte[] getBytes(final int layerNum) {
		final int layerCount = byteLayers.size();
		if(layerNum >= layerCount) {
			ByteBuffer prevLayer = byteLayers.get(layerCount - 1), nextLayer;
			long prevSeed, nextSeed;
			final int ringSize = prevLayer.array().length;
			for(int i = layerCount; i <= layerNum; i ++) {
				nextLayer = ByteBuffer.allocate(ringSize);
				prevSeed = prevLayer.getLong(0);
				nextSeed = Long.reverse(nextWord(Long.reverseBytes(prevSeed)));
				LOG.debug(Markers.MSG, String.format(MSG_FMT_NEW_LAYER, i, prevSeed, nextSeed));
				generateData(nextLayer, nextSeed);
				byteLayers.add(nextLayer);
				prevLayer = nextLayer;
			}
		}
		return byteLayers.get(layerNum).array();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
