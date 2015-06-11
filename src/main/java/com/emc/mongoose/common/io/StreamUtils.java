package com.emc.mongoose.common.io;
//
import com.emc.mongoose.common.logging.LogUtil;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
/**
 Created by kurila on 17.03.15.
 */
public final class StreamUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static long consumeQuietly(final InputStream contentStream, final int buffSize) {
		int n;
		long sum = 0;
		final byte buff[] = new byte[buffSize];
		try {
			while(true) {
				n = contentStream.read(buff);
				if(n < 0) {
					break;
				} else {
					sum += n;
				}
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		return sum;
	}
	//
	public static long consumeQuietly(
		final ContentDecoder in, final IOControl ioCtl, final ByteBuffer bbuff
	) {
		int n;
		long sum = 0;
		try {
			while(!in.isCompleted()) {
				n = in.read(bbuff);
				if(n < 0) {
					break;
				} else {
					sum += n;
				}
				bbuff.clear();
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		return sum;
	}
	//
}
