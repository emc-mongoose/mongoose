package com.emc.mongoose.core.impl.data.content;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.core.api.data.content.ContentSource;
//
import java.io.IOException;
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
	protected final ByteBuffer zeroByteLayer;
	protected final transient List<ByteBuffer> byteLayers = new ArrayList<>();
	protected final int size;
	//
	protected ContentSourceBase(final ByteBuffer zeroByteLayer) {
		this.zeroByteLayer = zeroByteLayer;
		byteLayers.add(zeroByteLayer);
		this.size = zeroByteLayer.capacity();
	}
	//
	protected ContentSourceBase(final ReadableByteChannel zeroLayerSrcChan, final int size)
		throws IOException {
		this.zeroByteLayer = ByteBuffer.allocateDirect(size);
		byteLayers.add(zeroByteLayer);
		this.size = size;
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
