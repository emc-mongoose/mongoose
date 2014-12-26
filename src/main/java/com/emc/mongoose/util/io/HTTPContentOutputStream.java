package com.emc.mongoose.util.io;
//
import com.emc.mongoose.util.pool.InstancePool;
import com.emc.mongoose.util.pool.Reusable;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
/**
 Created by kurila on 09.12.14.
 */
public final class HTTPContentOutputStream
extends OutputStream
implements Reusable {
	//
	private volatile ByteBuffer bb = null;
	private volatile byte[] bs = null; // Invoker's previous array
	private volatile byte[] b1 = new byte[1];
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
			do {
				n = out.write(bb);
				if(n > 0) {
					break;
				} else {
					ioCtl.requestOutput();
				}
			} while(true);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Instances pooling ///////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<HTTPContentOutputStream>
		POOL = new InstancePool<>(HTTPContentOutputStream.class);
	//
	public static HTTPContentOutputStream getInstance(
		final ContentEncoder out, final IOControl ioCtl
	) {
		return POOL.take(out, ioCtl);
	}
	//
	@Override
	public final void close()
	throws IOException {
		out.complete();
		POOL.release(this);
	}
	//
	@Override
	public final HTTPContentOutputStream reuse(final Object... args) {
		if(args.length > 0) {
			out = ContentEncoder.class.cast(args[0]);
		}
		if(args.length > 1) {
			ioCtl = IOControl.class.cast(args[1]);
		}
		return this;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
}
