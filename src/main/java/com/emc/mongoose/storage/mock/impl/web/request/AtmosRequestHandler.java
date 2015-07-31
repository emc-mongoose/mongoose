package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
// mongoose-storage-adapter-atmos.jar
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
//
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
//
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by andrey on 13.05.15.
 */
public final class AtmosRequestHandler<T extends WSObjectMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String URI_BASE_PATH = "/rest";
	//
	public AtmosRequestHandler(final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage) {
		super(runTimeConfig, sharedStorage);
	}
	//
	public boolean matches(final String requestURI) {
		return requestURI != null && requestURI.startsWith(URI_BASE_PATH);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
	) {
		final String subtenant = httpRequest.containsHeader(SubTenant.KEY_SUBTENANT_ID) ?
			httpRequest.getLastHeader(SubTenant.KEY_SUBTENANT_ID).getValue() : null;
		if(requestURI.length > 2) {
			if(requestURI[2].equals(WSSubTenantImpl.SUBTENANT)) { // subtenant-related request
				handleGenericContainerReq(
					httpRequest, httpResponse,
					METHOD_PUT.equals(method) ? randomString(16) : subtenant,
					method, dataId
				);
			} else {
				if(requestURI[2].equals(WSRequestConfigImpl.API_TYPE_OBJ)) {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG,
							"Handle atmos object request. URI doesn't contain the object ID."
						);
					}
					if(method.equals(METHOD_POST)) {
						final String
							newDataId = generateId(),
							headerLocation = httpRequest.getRequestLine().getUri()+"/"+newDataId;
						httpResponse.setHeader(HttpHeaders.LOCATION, headerLocation);
						handleGenericDataReq(
							httpRequest, httpResponse, method, subtenant, newDataId
						);
					}
				} else {
					if(LOG.isTraceEnabled(Markers.MSG)) {
						LOG.trace(
							Markers.MSG, "Handle atmos request. URI contains the object ID."
						);
					}
					handleGenericDataReq(
						httpRequest, httpResponse, method, subtenant, dataId
					);
				}
			}
		} else {
			httpResponse.setStatusCode(HttpStatus.SC_BAD_REQUEST);
		}
	}
	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	) {
		resp.setStatusCode(HttpStatus.SC_NOT_IMPLEMENTED);
	}
}
