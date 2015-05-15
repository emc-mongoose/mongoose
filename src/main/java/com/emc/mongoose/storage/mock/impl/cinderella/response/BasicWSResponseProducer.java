package com.emc.mongoose.storage.mock.impl.cinderella.response;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.io.HTTPOutputStream;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
//
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
//
import org.apache.http.nio.protocol.HttpAsyncResponseProducer;
import org.apache.http.protocol.HttpContext;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.OutputStream;
/**
 * Created by olga on 12.02.15.
 */
public final class BasicWSResponseProducer
implements HttpAsyncResponseProducer, Reusable<BasicWSResponseProducer> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile HttpResponse response = null;
	//
	//private final byte buff[] = new byte[(int) RunTimeConfig.getContext().getDataBufferSize()];
	//private final ByteBuffer bb = ByteBuffer.wrap(buff);
	//
	//public BasicResponseProducer(HttpResponse response) {
	//	this.response = response;
	//}
	//
	@Override
	public HttpResponse generateResponse() {
		return this.response;
	}
	//
	@Override
	public final void produceContent(
		final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		try(final OutputStream outStream = HTTPOutputStream.getInstance(encoder, ioctrl)) {
			final HttpEntity entity = this.response.getEntity();
			if(entity != null) {
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Write out {} bytes",
						entity.getContentLength()
					);
				}
				entity.writeTo(outStream);
			}
		} catch(final InterruptedException ignored) {
		} finally {
			encoder.complete();
		}
		/*
		final HttpEntity respEntity = this.response.getEntity();
		if(respEntity != null) {
			long contentLength = respEntity.getContentLength();
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.MSG, "Write out {} bytes", contentLength);
			}
			long byteCountToWrite = contentLength;
			int n;
			try(final InputStream dataStream = respEntity.getContent()) {
				while(byteCountToWrite > 0) {
					n = byteCountToWrite < buff.length ? (int) byteCountToWrite : buff.length;
					if(0 >= dataStream.read(buff, 0, n)) {
						break;
					}
					bb.rewind().limit(n);
					while(bb.hasRemaining()) {
						encoder.write(bb);
					}
					byteCountToWrite -= n;
				}

			} finally {
				encoder.complete();
				//this.producer.close();
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "{} bytes written out",
						contentLength - byteCountToWrite
					);
				}
			}
		}
		*/
	}
	//
	@Override
	public final void responseCompleted(final HttpContext context) {
	}
	//
	@Override
	public final void failed(final Exception e) {
		LogUtil.failure(LOG, Level.WARN, e, "Response failure");
	}
	//
	@Override
	public final void close()
	throws IOException {
		release();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<BasicWSResponseProducer> POOL = new InstancePool<>(
		BasicWSResponseProducer.class
	);
	//
	public static BasicWSResponseProducer getInstance(final HttpResponse response) {
		return POOL.take(response);
	}
	//
	@Override
	public final Reusable<BasicWSResponseProducer> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		if(args != null) {
			if(args.length > 0) {
				response = HttpResponse.class.cast(args[0]);
			}
		}
		return this;
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}
}
