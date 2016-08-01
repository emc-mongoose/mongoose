package com.emc.mongoose.storage.driver.http.s3;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.api.io.task.DataIoTask;
import com.emc.mongoose.model.api.io.task.IoTask;
import com.emc.mongoose.model.api.item.Item;
import com.emc.mongoose.storage.driver.http.base.HttpDriverBase;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.log.LogUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 Created by kurila on 01.08.16.
 */
public final class HttpS3Driver<I extends Item, O extends IoTask<I>>
extends HttpDriverBase<I, O> {
	
	private final static Logger LOG = LogManager.getLogger();
	
	protected HttpS3Driver(
		final Config.LoadConfig loadConfig, final Config.StorageConfig storageConfig,
		final Config.SocketConfig socketConfig
	) throws UserShootHisFootException {
		super(loadConfig, storageConfig, socketConfig);
	}
	
	@Override
	protected SimpleChannelInboundHandler<HttpObject> getApiSpecificHandler() {
		return new HttpS3Handler();
	}
	
	@Override
	protected HttpRequest getDataRequest(final O ioTask) {
		return null;
	}
	
	@Override
	protected HttpRequest getRequest(final O ioTask) {
		return null;
	}
}
