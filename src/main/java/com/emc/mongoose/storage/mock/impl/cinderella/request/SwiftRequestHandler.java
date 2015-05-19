package com.emc.mongoose.storage.mock.impl.cinderella.request;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.Map;
/**
 Created by andrey on 13.05.15.
 */
public final class SwiftRequestHandler
extends WSRequestHandlerBase {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String AUTH = "auth";
	//
	private final String apiBasePathSwift;
	//
	public SwiftRequestHandler(
		final RunTimeConfig runTimeConfig, final Map<String, WSObjectMock> sharedStorage,
	    final IOStats ioStats
	) {
		super(runTimeConfig, sharedStorage, ioStats);
		apiBasePathSwift = runTimeConfig.getString(WSRequestConfigImpl.KEY_CONF_SVC_BASEPATH);
	}
	//
	public boolean matches(final String requestURI) {
		return requestURI != null &&
			(requestURI.startsWith(AUTH, 1) || requestURI.startsWith(apiBasePathSwift, 1));
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
	) {
		if(requestURI.length > 1) {
			if(requestURI[1].equals(AUTH)) { // create an auth token
				final String authToken = randomString(5);
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(LogUtil.MSG, "Created auth token: {}", authToken);
				}
				httpResponse.setHeader(WSRequestConfigImpl.KEY_X_AUTH_TOKEN, authToken);
				httpResponse.setStatusCode(HttpStatus.SC_OK);
			} else if(
				requestURI[1].equals(apiBasePathSwift) && requestURI.length == 4 &&
				method.equalsIgnoreCase(METHOD_PUT)
			) { // put the container
				httpResponse.setStatusCode(HttpStatus.SC_OK);
				if(LOG.isTraceEnabled(LogUtil.MSG)) {
					LOG.trace(
						LogUtil.MSG, "Create the container: {}", requestURI[requestURI.length - 1]
					);
				}
			} else {
				handleGenericDataReq(httpRequest, httpResponse, method, dataId);
			}
		} else {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
	}
}
