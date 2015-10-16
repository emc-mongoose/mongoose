package com.emc.mongoose.core.impl.data.content;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 16.10.15.
 */
public abstract class ContentSourceBase
implements ContentSource {
	//
	protected ByteBuffer zeroByteLayer;
	protected final List<ByteBuffer> byteLayers = new ArrayList<>();
	//
	protected ContentSourceBase(final ByteBuffer zeroByteLayer) {
		this.zeroByteLayer = zeroByteLayer;
		byteLayers.add(zeroByteLayer);
	}
	//
	protected ContentSourceBase(final ReadableByteChannel zeroLayerSrcChan, final int size)
	throws IOException {
		this.zeroByteLayer = ByteBuffer.allocateDirect(size);
		byteLayers.add(zeroByteLayer);
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
	@Override
	public final int getSize() {
		return zeroByteLayer.capacity();
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
	//
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		final byte buff[] = new byte[zeroByteLayer.capacity()];
		zeroByteLayer.get(buff);
		out.write(zeroByteLayer.capacity());
		out.write(buff);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final int size = in.readInt();
		final byte buff[] = new byte[size];
		int n = 0, m;
		do {
			m = in.read(buff, n, size - n);
			if(m < 0) {
				throw new EOFException("Unexpected end of stream");
			} else {
				n += m;
			}
		} while(size < n);
		//
		zeroByteLayer = ByteBuffer.allocateDirect(size).put(buff);
		byteLayers.clear();
		byteLayers.add(zeroByteLayer);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static ContentSourceBase DEFAULT = null;
	private final static Lock LOCK = new ReentrantLock();
	public static ContentSourceBase getDefault() {
		LOCK.lock();
		try {
			if(DEFAULT == null) {
				try {
					DEFAULT = new FileContentSource();
				} catch(final Exception e) {
					DEFAULT = new UniformContentSource(
						UniformContentSource.DEFAULT_SEED, Constants.BUFF_SIZE_HI
					);
				}
			}
		} finally {
			LOCK.unlock();
		}
		return DEFAULT;
	}
}
