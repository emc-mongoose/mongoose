package com.emc.mongoose.common.io;
// mongoose-common.jar
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.collections.InstancePool;
//
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.logging.LogUtil;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
/**
 Created by kurila on 09.12.14.
 */
public final class HTTPInputStream
extends InputStream
implements Reusable<HTTPInputStream> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static int BYTE_VALUE_MAX = 0xff;
	//
	private volatile ByteBuffer bb = null;
	private volatile byte[] bs = null; // Invoker's previous array
	private volatile byte[] b1 = new byte[1];
	private volatile ContentDecoder in = null;
	private volatile IOControl ioCtl = null;
	//
	@Override
	public final synchronized int read()
	throws IOException {
		return read(b1) == 1 ? b1[0] & BYTE_VALUE_MAX : -1;
	}
	//
	@Override
	public final synchronized int read(final byte bs[], final int off, final int len)
	throws IOException, IndexOutOfBoundsException {
		//
		if(
			(off < 0) || (off > bs.length) || (len < 0) || ((off + len) > bs.length) ||
			((off + len) < 0)
		) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}
		//
		if(this.bs != bs) {
			this.bs = bs;
			this.bb = ByteBuffer.wrap(bs);
		}
		//
		bb.limit(Math.min(off + len, bb.capacity()));
		bb.position(off);
		//
		return in.read(bb);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Instances pooling ///////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<HTTPInputStream>
		POOL = new InstancePool<>(HTTPInputStream.class);
	//
	public static HTTPInputStream getInstance(
		final ContentDecoder in, final IOControl ioCtl
	) throws InterruptedException {
		return POOL.take(in, ioCtl);
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}
	//
	@Override
	public final HTTPInputStream reuse(final Object... args) {
		if(args.length > 0) {
			in = ContentDecoder.class.cast(args[0]);
		}
		if(args.length > 1) {
			ioCtl = IOControl.class.cast(args[1]);
		}
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	/**
	 * Does not close the stream actually.
	 * Puts the instance back to the pool for further reusing
	 */
	@Override
	public final void close() {
		release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public static void consumeQuietly(
		final ContentDecoder in, final IOControl ioCtl, final ByteBuffer bbuff
	) {
		try {
			while(!in.isCompleted() && in.read(bbuff) >= 0) {
				bbuff.clear();
			}
		} catch(final IOException ignore) {
		}
	}
}
