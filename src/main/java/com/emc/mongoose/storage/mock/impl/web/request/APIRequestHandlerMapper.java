package com.emc.mongoose.storage.mock.impl.web.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.storage.mock.api.ObjectStorage;
//
import com.emc.mongoose.storage.mock.impl.web.data.BasicWSObjectMock;
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
public final class APIRequestHandlerMapper<T extends BasicWSObjectMock>
implements HttpAsyncRequestHandlerMapper {
	//
	//private final static Logger LOG = LogManager.getLogger();
	//
	private final AtmosRequestHandler<T> reqHandlerAtmos;
	private final S3RequestHandler<T> reqHandlerS3;
	private final SwiftRequestHandler<T> reqHandlerSwift;
	//
	public APIRequestHandlerMapper(
		final RunTimeConfig runTimeConfig, final ObjectStorage<T> sharedStorage
	) {
		reqHandlerAtmos = new AtmosRequestHandler<>(runTimeConfig, sharedStorage);
		reqHandlerS3 =  new S3RequestHandler<>(runTimeConfig, sharedStorage);
		reqHandlerSwift = new SwiftRequestHandler<>(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public final HttpAsyncRequestHandler<HttpRequest> lookup(final HttpRequest httpRequest) {
		final String requestURI = httpRequest.getRequestLine().getUri();
		if(reqHandlerAtmos.matches(requestURI)) {
			return reqHandlerAtmos;
		} else if(reqHandlerSwift.matches(requestURI)) {
			return reqHandlerSwift;
		} else {
			return reqHandlerS3;
		}
	}
}
