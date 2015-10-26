package com.emc.mongoose.common.net.http;
//
import com.emc.mongoose.common.io.IOWorker;
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.http.nio.ContentDecoder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.ByteBuffer;
/**
 Created by andrey on 13.09.15.
 */
public class ContentUtil {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static  int consumeQuietly(final ContentDecoder in, final long expectedByteCount) {
		int doneByteCount = 0;
		try {
			if(!in.isCompleted()) {
				final ByteBuffer buff = ((IOWorker) Thread.currentThread())
					.getThreadLocalBuff(expectedByteCount);
				doneByteCount = in.read(buff);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		return doneByteCount < 0 ? 0 : doneByteCount;
	}
}
