package com.emc.mongoose.storage.mock.impl.http.request;
//
//import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.storage.mock.api.ReqUriMatchingHandler;
import com.emc.mongoose.storage.mock.api.HttpStorageMock;
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
	private final ReqUriMatchingHandler<T> reqHandlerAtmos;
	private final ReqUriMatchingHandler<T> reqHandlerS3;
	private final ReqUriMatchingHandler<T> reqHandlerSwift;
	//
	public APIRequestHandlerMapper(
		final AppConfig appConfig, final HttpStorageMock<T> sharedStorage
	) {
		reqHandlerAtmos = new AtmosRequestHandler<>(appConfig, sharedStorage);
		reqHandlerS3 =  new S3RequestHandler<>(appConfig, sharedStorage);
		reqHandlerSwift = new SwiftRequestHandler<>(appConfig, sharedStorage);
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
