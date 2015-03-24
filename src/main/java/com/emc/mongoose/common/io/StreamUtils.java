package com.emc.mongoose.common.io;
//
import com.emc.mongoose.common.logging.TraceLogger;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
/**
 Created by kurila on 17.03.15.
 */
public final class StreamUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static void skipStreamDataQuietly(final InputStream contentStream, final int maxPageSize) {
		final byte buff[] = new byte[maxPageSize];
		try {
			while(contentStream.read(buff) != -1);
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "Content reading failure");
		}
	}
}
