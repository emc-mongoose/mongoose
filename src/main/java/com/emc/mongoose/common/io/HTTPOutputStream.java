package com.emc.mongoose.common.io;
// mongoose-common.jar
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.pool.Reusable;
import com.emc.mongoose.common.pool.InstancePool;
//
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;
/**
 Created by kurila on 09.12.14.
 */
public final class HTTPOutputStream
extends OutputStream
implements Reusable {
	//
	private final static Logger LOG = LogManager.getLogger();
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
		while(bb.remaining() > 0) {
			out.write(bb);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Instances pooling ///////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<HTTPOutputStream>
		POOL = new InstancePool<>(HTTPOutputStream.class);
	//
	public static HTTPOutputStream getInstance(
		final ContentEncoder out, final IOControl ioCtl
	) throws InterruptedException {
		return POOL.take(out, ioCtl);
	}
	//
	private final AtomicBoolean isClosed = new AtomicBoolean(true);
	//
	@Override
	public final void release() {
		if(isClosed.compareAndSet(false, true)) {
			if(!out.isCompleted()) {
				try {
					out.complete();
				} catch(final IOException e) {
					LogUtil.failure(LOG, Level.WARN, e, "Failed to finish the output stream");
				}
			}
			POOL.release(this);
		}
	}
	//
	@Override
	public final HTTPOutputStream reuse(final Object... args) {
		if(isClosed.compareAndSet(true, false)) {
			if(args.length > 0) {
				out = ContentEncoder.class.cast(args[0]);
			}
			if(args.length > 1) {
				ioCtl = IOControl.class.cast(args[1]);
			}
		}
		return this;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int compareTo(Reusable another) {
		return another == null ? 1 : hashCode() - another.hashCode();
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
}
