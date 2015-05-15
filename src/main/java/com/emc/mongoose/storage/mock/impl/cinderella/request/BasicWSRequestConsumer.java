package com.emc.mongoose.storage.mock.impl.cinderella.request;
//
import com.emc.mongoose.common.collections.InstancePool;
import com.emc.mongoose.common.collections.Reusable;
import com.emc.mongoose.common.io.HTTPInputStream;
//
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
//
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.BasicAsyncRequestConsumer;
import org.apache.logging.log4j.Level;

import java.nio.ByteBuffer;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 * Created by olga on 04.02.15.
 */
public final class BasicWSRequestConsumer
extends BasicAsyncRequestConsumer
implements Reusable<BasicWSRequestConsumer> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private volatile int buffSize = 0;
	private volatile ByteBuffer bbuff;
	//
	public BasicWSRequestConsumer() {
		super();
	}
	//
	@Override
	protected final void onEntityEnclosed(
		final HttpEntity entity, final ContentType contentType)
	{
		final long len = entity.getContentLength();
		//
		if(len < LoadExecutor.BUFF_SIZE_LO) {
			buffSize = LoadExecutor.BUFF_SIZE_LO;
			bbuff = ByteBuffer.allocate(buffSize);
		} else if(len > LoadExecutor.BUFF_SIZE_HI) {
			buffSize = LoadExecutor.BUFF_SIZE_HI;
			bbuff = ByteBuffer.allocate(buffSize);
		} else if(len != buffSize) {
			buffSize = (int) len;
			bbuff = ByteBuffer.allocate(buffSize);
		}
	}
	//
	@Override
	protected final void onContentReceived(final ContentDecoder decoder, final IOControl ioctrl) {
		try {
			HTTPInputStream.consumeQuietly(decoder, ioctrl, bbuff);
		} catch(final Throwable e) {
			LogUtil.failure(LOG, Level.WARN, e, "Content consuming failure");
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Reusable implementation /////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static InstancePool<BasicWSRequestConsumer> POOL = new InstancePool<>(
		BasicWSRequestConsumer.class
	);
	//
	public static BasicWSRequestConsumer getInstance() {
		return POOL.take();
	}
	//
	@Override
	public final Reusable<BasicWSRequestConsumer> reuse(final Object... args)
	throws IllegalArgumentException, IllegalStateException {
		return this;
	}
	//
	@Override
	public final void release() {
		POOL.release(this);
	}
}
