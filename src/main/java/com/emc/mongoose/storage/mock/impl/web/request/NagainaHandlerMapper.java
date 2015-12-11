package com.emc.mongoose.storage.mock.impl.web.request;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;

import static io.netty.handler.codec.http.HttpHeaders.Names.AUTHORIZATION;

@Sharable
public class NagainaHandlerMapper<T extends WSObjectMock> extends ChannelInboundHandlerAdapter {

	private final static String
			SWIFT_AUTH = "auth",
			S3_AUTH_PREFIX = RunTimeConfig.getContext().getApiS3AuthPrefix() + " ",
			ATMOS_URI_BASE_PATH = "/rest";

	private final RunTimeConfig runTimeConfig;
	private final WSMock<T> sharedStorage;
	private final String apiBasePathSwift;

	public NagainaHandlerMapper(RunTimeConfig runTimeConfig, WSMock<T> sharedStorage) {
		this.runTimeConfig = runTimeConfig;
		this.sharedStorage = sharedStorage;
		apiBasePathSwift = runTimeConfig.getString(WSRequestConfigImpl.KEY_CONF_SVC_BASEPATH);
	}

	private boolean checkS3(HttpRequest request) {
		String auth = request.headers().get(AUTHORIZATION);
		return auth != null && auth.startsWith(S3_AUTH_PREFIX);
	}

	private boolean checkSwift(HttpRequest request) {
		String uri = request.getUri();
		return uri.startsWith(SWIFT_AUTH, 1) || uri.startsWith(apiBasePathSwift, 1);
	}

	private boolean checkAtmos(HttpRequest request) {
		return request.getUri().startsWith(ATMOS_URI_BASE_PATH);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof HttpRequest) {
			if (checkS3((HttpRequest) msg)) {
				addHandler(ctx, new NagainaS3RequestHandler<>(runTimeConfig, sharedStorage));
			}
			if (checkSwift((HttpRequest) msg)) {
				addHandler(ctx, new NagainaSwiftRequestHandler<>(runTimeConfig, sharedStorage));
			}
			if (checkAtmos((HttpRequest) msg)) {
				addHandler(ctx, new NagainaAtmosRequestHandler<>(runTimeConfig, sharedStorage));
			}
			ctx.pipeline().remove(this);
			ctx.fireChannelRead(msg);
		}

	}

	private void addHandler(ChannelHandlerContext ctx, NagainaRequestHandlerBase<T> handler) {
		ctx.pipeline().addLast(handler);
	}
}
