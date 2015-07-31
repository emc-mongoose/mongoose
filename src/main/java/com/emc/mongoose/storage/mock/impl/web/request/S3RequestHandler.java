package com.emc.mongoose.storage.mock.impl.web.request;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
// mongoose-storage-mock.jar
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.mock.api.ContainerMockNotFoundException;
import com.emc.mongoose.storage.mock.api.WSMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by andrey on 13.05.15.
 */
public final class S3RequestHandler<T extends WSObjectMock>
extends WSRequestHandlerBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		MAX_KEYS = "maxKeys", MARKER = "marker";
	private final static Pattern
		PATTERN_MAX_KEYS = Pattern.compile(Bucket.URL_ARG_MAX_KEYS + "=(?<" + MAX_KEYS +  ">[\\d]+)&?"),
		PATTERN_MARKER = Pattern.compile(Bucket.URL_ARG_MARKER + "=(?<" + MARKER + ">[a-z\\d]+)&?");
	//
	public S3RequestHandler(final RunTimeConfig runTimeConfig, final WSMock<T> sharedStorage) {
		super(runTimeConfig, sharedStorage);
	}
	//
	@Override
	public final void handleActually(
		final HttpRequest httpRequest, final HttpResponse httpResponse, final String method,
		final String requestURI[], final String dataId
	) {
		final String bucketName;
		if(requestURI.length == 2) {
			bucketName = requestURI[requestURI.length - 1];
			handleGenericContainerReq(httpRequest, httpResponse, method, bucketName, dataId);
		} else {
			bucketName = requestURI[requestURI.length - 2];
			handleGenericDataReq(httpRequest, httpResponse, method, bucketName, dataId);
		}
	}
	//
	@Override
	protected final void handleContainerList(
		final HttpRequest req, final HttpResponse resp, final String name, final String dataId
	) {
		final String uri = req.getRequestLine().getUri();
		int maxCount = -1;
		String marker = null;
		final Matcher maxKeysMatcher = PATTERN_MAX_KEYS.matcher(uri);
		if(maxKeysMatcher.find()) {
			try {
				maxCount = Integer.parseInt(maxKeysMatcher.group(MAX_KEYS));
			} catch(final NumberFormatException e) {
				LOG.warn(Markers.ERR, "Failed to parse max keys argument value in the URI: " + uri);
			}
		}
		final Matcher markerMatcher = PATTERN_MARKER.matcher(uri);
		if(markerMatcher.find()) {
			try {
				marker = markerMatcher.group(MARKER);
			} catch(final IllegalArgumentException ignored) {
			}
		}
		//
		if(maxCount <= 0) {
			maxCount = batchSize;
		}
		//
		final List<T> buff = new ArrayList<>(maxCount);
		try {
			marker = sharedStorage.list(name, marker, buff, maxCount);
		} catch(final ContainerMockNotFoundException e) {
			resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
			return;
		}
		//

	}
}
