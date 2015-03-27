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
	public static void skipStreamDataQuietly(final InputStream contentStream) {
		try {
			long n = contentStream.available();
			while(n > 0) {
				contentStream.skip(n);
				n = contentStream.available();
			}
		} catch(final IOException e) {
			TraceLogger.failure(LOG, Level.WARN, e, "Failed to skip the input stream data");
		}
	}
}
