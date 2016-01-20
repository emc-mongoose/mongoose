package com.emc.mongoose.storage.mock.impl.web.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.storage.mock.api.ReqURIMatchingHandler;
import com.emc.mongoose.storage.mock.api.WSMock;
//
import com.emc.mongoose.storage.mock.api.HttpDataItemMock;
import org.apache.http.HttpRequest;
//
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
//
//import org.apache.log.log4j.LogManager;
//import org.apache.log.log4j.Logger;
/**
 Created by andrey on 13.05.15.
 */
public final class APIRequestHandlerMapper<T extends HttpDataItemMock>
implements HttpAsyncRequestHandlerMapper {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final ReqURIMatchingHandler<T> reqHandlerAtmos;
	private final ReqURIMatchingHandler<T> reqHandlerS3;
	private final ReqURIMatchingHandler<T> reqHandlerSwift;
	//
	public APIRequestHandlerMapper(
		final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage
	) {
		reqHandlerAtmos = new AtmosRequestHandler<>(runTimeConfig, sharedStorage);
		reqHandlerS3 =  new S3RequestHandler<>(runTimeConfig, sharedStorage);
		reqHandlerSwift = new SwiftRequestHandler<>(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public final HttpAsyncRequestHandler<HttpRequest> lookup(final HttpRequest httpRequest) {
		if(reqHandlerAtmos.matches(httpRequest)) {
			return reqHandlerAtmos;
		} else if(reqHandlerSwift.matches(httpRequest)) {
			return reqHandlerSwift;
		} else {
			return reqHandlerS3;
		}
	}
}
