package com.emc.mongoose.storage.mock.impl.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
//
import org.apache.http.HttpRequest;
//
import org.apache.http.nio.protocol.HttpAsyncRequestHandler;
import org.apache.http.nio.protocol.HttpAsyncRequestHandlerMapper;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Collections;
import java.util.Map;
/**
 Created by andrey on 13.05.15.
 */
public final class APIRequestHandlerMapper
implements HttpAsyncRequestHandlerMapper {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final AtmosRequestHandler reqHandlerAtmos;
	private final S3RequestHandler reqHandlerS3;
	private final SwiftRequestHandler reqHandlerSwift;
	//
	private final Map<String, WSObjectMock> sharedStorage;
	//
	public APIRequestHandlerMapper(
		final RunTimeConfig runTimeConfig, final Map<String, WSObjectMock> storage
	) {
		this.sharedStorage = Collections.synchronizedMap(storage);
		//
		reqHandlerAtmos = new AtmosRequestHandler(runTimeConfig, this.sharedStorage);
		reqHandlerS3 =  new S3RequestHandler(runTimeConfig, this.sharedStorage);
		reqHandlerSwift = new SwiftRequestHandler(runTimeConfig, this.sharedStorage);
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
