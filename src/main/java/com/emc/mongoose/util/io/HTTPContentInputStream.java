package com.emc.mongoose.util.io;
//
import com.emc.mongoose.util.pool.BasicInstancePool;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
//
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
/**
 Created by kurila on 09.12.14.
 */
public final class HTTPContentInputStream
extends InputStream {
	//
	private volatile ByteBuffer bb = null;
	private volatile byte[] bs = null; // Invoker's previous array
	private volatile byte[] b1 = new byte[1];
	private volatile ContentDecoder in = null;
	private volatile IOControl ioCtl = null;
	//
	private final static int BYTE_VALUE_MAX = 0xff;
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
	private final static BasicInstancePool<HTTPContentInputStream>
		POOL = new BasicInstancePool<>(HTTPContentInputStream.class);
	//
	public static HTTPContentInputStream getInstance(
		final ContentDecoder in, final IOControl ioCtl
	) {
		final HTTPContentInputStream instance = POOL.take();
		instance.in = in;
		instance.ioCtl = ioCtl;
		return instance;
	}
	//
	@Override
	public final void close() {
		POOL.release(this);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
