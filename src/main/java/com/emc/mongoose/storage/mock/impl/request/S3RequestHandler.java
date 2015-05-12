package com.emc.mongoose.storage.mock.impl.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Map;
/**
 Created by andrey on 13.05.15.
 */
public final class S3RequestHandler
extends WSRequestHandlerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public S3RequestHandler(
		final RunTimeConfig runTimeConfig, final Map<String, WSObjectMock> sharedStorage
	) {
		super(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
	) {
		if(method.equals(METHOD_PUT) && requestURI.length == 2) {
			httpResponse.setStatusCode(HttpStatus.SC_OK);
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(LogUtil.MSG, "Created bucket: {}", requestURI[requestURI.length - 1]);
			}
		} else {
			handleGenericDataReq(httpRequest, httpResponse, method, dataId);
		}
	}
}
