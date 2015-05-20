package com.emc.mongoose.common.io;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
//
import org.apache.http.nio.ContentDecoder;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
/**
 Created by kurila on 20.05.15.
 */
public final class HTTPContentDecoderChannel
implements ReadableByteChannel, Reusable<HTTPContentDecoderChannel> {
	//
	private ContentDecoder contentDecoder;
	//
	@Override
	public final int read(final ByteBuffer src)
	throws IOException {
		return contentDecoder.read(src);
	}
	//
	@Override
	public final void close() {
		release();
	}
	//
	@Override
	public final boolean isOpen() {
		return true;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	private static final InstancePool<HTTPContentDecoderChannel>
		INSTANCE_POOL = new InstancePool<>(HTTPContentDecoderChannel.class);
	//
	public static HTTPContentDecoderChannel getInstance(final ContentDecoder contentEncoder) {
		return INSTANCE_POOL.take(contentEncoder);
	}
	//
	@Override
	public final Reusable<HTTPContentDecoderChannel> reuse(final Object... args)
		throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				contentDecoder = ContentDecoder.class.cast(args[0]);
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		INSTANCE_POOL.release(this);
	}
}
