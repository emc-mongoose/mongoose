package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.http.base.HttpDriverBase;

import static com.emc.mongoose.model.api.item.Item.SLASH;
import static com.emc.mongoose.storage.driver.http.s3.S3Constants.KEY_X_AMZ_COPY_SOURCE;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.SocketConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URISyntaxException;

/**
 Created by kurila on 01.08.16.
 */
public final class HttpS3Driver<I extends Item, O extends IoTask<I>>
extends HttpDriverBase<I, O> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	public HttpS3Driver(
		final String runId, final LoadConfig loadConfig, final StorageConfig storageConfig,
		final SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(runId, loadConfig, storageConfig, socketConfig);
	}
	
	@Override
	protected final SimpleChannelInboundHandler<HttpObject> getApiSpecificHandler() {
		return new HttpS3Handler();
	}
	
	@Override
	protected void applyCopyHeaders(final HttpHeaders httpHeaders, final I obj)
	throws URISyntaxException {
		httpHeaders.set(KEY_X_AMZ_COPY_SOURCE, getObjectSrcPath(obj));
	}
	
	@Override
	protected final String getObjectSrcPath(final I object)
	throws URISyntaxException {
		if(object == null) {
			throw new IllegalArgumentException("No object");
		}
		final String objPath = object.getPath();
		if(objPath.endsWith(SLASH)) {
			return objPath + object.getName();
		} else {
			return objPath + SLASH + object.getName();
		}
	}
}
