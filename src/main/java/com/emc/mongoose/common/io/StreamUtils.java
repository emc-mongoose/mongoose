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
	public static void consumeQuietly(final InputStream contentStream, final int buffSize) {
		final byte buff[] = new byte[buffSize];
		try {
			while(contentStream.read(buff) != -1);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
	}
	//
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
	//
}
