package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.io.HTTPOutputStream;
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.EntityAsyncContentProducer;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.nio.protocol.BasicAsyncResponseProducer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Created by olga on 12.02.15.
 */
public final class BasicResponseProducer
extends BasicAsyncResponseProducer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final HttpResponse response;
	//
	private final byte buff[] = new byte[(int) RunTimeConfig.getContext().getDataBufferSize()];
	private final ByteBuffer bb = ByteBuffer.wrap(buff);
	//
	public BasicResponseProducer(HttpResponse response) {
		super(response);
		this.response = response;
	}
	//
	@Override
	public final void produceContent(
		final ContentEncoder encoder, final IOControl ioctrl)
	throws IOException {
		/*
		try(final OutputStream outStream = HTTPOutputStream.getInstance(encoder, ioctrl)) {
			final HttpEntity entity = this.response.getEntity();
			if( entity != null) {
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Write out {} bytes",
						entity.getContentLength()
					);
				}
				entity.writeTo(outStream);
			}
		} catch(final InterruptedException e) {
			// do nothing
		} finally {
			encoder.complete();
			this.producer.close();
		}
		*/
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
	}
	/*
	@Override
	public final void close()
	throws IOException {
		if (this.producer != null) {
			this.producer.close();
		}
	}
	//
	@Override
	public final String toString() {
		final StringBuilder buf = new StringBuilder();
		buf.append(this.response);
		if (this.producer != null) {
			buf.append(" ").append(this.producer);
		}
		return buf.toString();
	}*/
}
