package com.emc.mongoose.common.io;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
//
import org.apache.http.nio.ContentEncoder;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
/**
 Created by kurila on 20.05.15.
 */
public final class HTTPContentEncoderChannel
implements WritableByteChannel, Reusable<HTTPContentEncoderChannel> {
	//
	private ContentEncoder contentEncoder;
	//
	@Override
	public final int write(final ByteBuffer src)
	throws IOException {
		return contentEncoder.write(src);
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
	private static final InstancePool<HTTPContentEncoderChannel>
		INSTANCE_POOL = new InstancePool<>(HTTPContentEncoderChannel.class);
	//
	public static HTTPContentEncoderChannel getInstance(final ContentEncoder contentEncoder) {
		return INSTANCE_POOL.take(contentEncoder);
	}
	//
	@Override
	public final Reusable<HTTPContentEncoderChannel> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				contentEncoder = ContentEncoder.class.cast(args[0]);
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
