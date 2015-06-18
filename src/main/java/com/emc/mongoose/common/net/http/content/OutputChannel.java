package com.emc.mongoose.common.net.http.content;
//
//import com.emc.mongoose.common.collections.InstancePool;
//import com.emc.mongoose.common.collections.Reusable;
//
import com.emc.mongoose.common.log.Markers;
import org.apache.http.nio.ContentEncoder;
//
//import org.apache.log.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
/**
 Created by kurila on 20.05.15.
 */
public final class OutputChannel
implements WritableByteChannel {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private ContentEncoder contentEncoder = null;
	//
	@Override
	public final int write(final ByteBuffer src)
	throws IOException {
		if(contentEncoder == null) {
			throw new IOException("The channel is not ready for the output");
		}
		return contentEncoder.write(src);
	}
	//
	@Override
	public final void close()
	throws IOException {
		if(contentEncoder != null) {
			contentEncoder = null;
		}
	}
	//
	@Override
	public final boolean isOpen() {
		return contentEncoder != null;
	}
	//
	public final void setContentEncoder(final ContentEncoder contentEncoder) {
		if(this.contentEncoder != null) {
			LOG.warn(Markers.ERR, "Possible output channel concurrent usage");
		}
		this.contentEncoder = contentEncoder;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	/*private static final InstancePool<HTTPContentEncoderChannel> INSTANCE_POOL;
	static {
		InstancePool<HTTPContentEncoderChannel> t = null;
		try {
			t = new InstancePool<>(HTTPContentEncoderChannel.class.getConstructor());
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LogManager.getLogger(), Level.FATAL, e, "No such constructor");
		}
		INSTANCE_POOL = t;
	}
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
	}*/
}
