package com.emc.mongoose.util.io;
//
import com.emc.mongoose.util.logging.Markers;
//
import com.emc.mongoose.util.pool.BasicInstancePool;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
/**
 Created by kurila on 09.12.14.
 */
public final class HTTPContentOutputStream
extends OutputStream {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private ByteBuffer bb = null;
	private byte[] bs = null; // Invoker's previous array
	private byte[] b1 = new byte[1];
	private volatile ContentEncoder out = null;
	private volatile IOControl ioCtl = null;
	//
	@Override
	public synchronized void write(final int b)
	throws IOException {
		b1[0] = (byte) b;
		write(b1);
	}
	//
	@Override
	public final synchronized void write(final byte bs[], final int off, final int len)
	throws IOException, IndexOutOfBoundsException {
		if(
			(off < 0) || (off > bs.length) || (len < 0) || ((off + len) > bs.length) ||
			((off + len) < 0)
		) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return;
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
		int n;
		while(bb.remaining() > 0) {
			n = out.write(bb);
			if(n <= 0) {
				if(LOG.isTraceEnabled(Markers.ERR)) {
					LOG.trace(Markers.ERR, "No bytes written");
				}
				ioCtl.requestOutput();
			}
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Instances pooling ///////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static BasicInstancePool<HTTPContentOutputStream>
		POOL = new BasicInstancePool<>(HTTPContentOutputStream.class);
	//
	public static HTTPContentOutputStream getInstance(
		final ContentEncoder out, final IOControl ioCtl
	) {
		final HTTPContentOutputStream instance = POOL.take();
		instance.out = out;
		instance.ioCtl = ioCtl;
		return instance;
	}
	//
	@Override
	public final void close()
	throws IOException {
		out.complete();
		POOL.release(this);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
