package com.emc.mongoose.common.net.http;
//
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_HI;
import static com.emc.mongoose.common.conf.Constants.BUFF_SIZE_LO;
//
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
 Created by kurila on 17.03.15.
 */
public final class IOUtils {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static int
		BUFF_COUNT = (int) (Math.log(BUFF_SIZE_HI / BUFF_SIZE_LO) / Math.log(2) + 1);
	//
	private static ThreadLocal<ByteBuffer[]>
		THREAD_LOCAL_IO_BUFFERS = new ThreadLocal<ByteBuffer[]>() {
			@Override
			protected final ByteBuffer[] initialValue() {
				return new ByteBuffer[BUFF_COUNT];
			}
		};
	//
	public static int consumeQuietlyNIO(final ContentDecoder in, final long expectedByteCount) {
		//
		final ByteBuffer ioBuffers[] = THREAD_LOCAL_IO_BUFFERS.get();
		//
		int doneByteCount = 0;
		try {
			if(!in.isCompleted()) {
				//
				int i, currBuffSize = BUFF_SIZE_LO;
				for(i = 0; i < ioBuffers.length && currBuffSize < expectedByteCount; i ++) {
					currBuffSize *= 2;
				}
				//
				ByteBuffer buff = ioBuffers[i == ioBuffers.length ? i - 1 : i];
				if(buff == null) {
					buff = ByteBuffer.allocateDirect(currBuffSize);
					ioBuffers[i] = buff;
				} else {
					buff.clear();
				}
				//
				doneByteCount = in.read(buff);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.DEBUG, e, "Content reading failure");
		}
		return doneByteCount;
	}
}
