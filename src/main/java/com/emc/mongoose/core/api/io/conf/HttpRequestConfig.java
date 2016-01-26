package com.emc.mongoose.core.api.io.conf;
// mongoose-core-api.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.io.task.IOTask;
// mongoose-common.jar
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.message.HeaderGroup;
//
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 29.09.14.
 An HTTP request shared configuration.
 */
public interface HttpRequestConfig<T extends HttpDataItem, C extends Container<T>>
extends RequestConfig<T, C> {
	//
	String
		KEY_EMC_ACCEPT = "x-emc-accept",
		KEY_EMC_FS_ACCESS = "x-emc-file-system-access-enabled",
		KEY_EMC_DATE = "x-emc-date",
		KEY_EMC_LIMIT = "x-emc-limit",
		KEY_EMC_NS = "x-emc-namespace",
		KEY_EMC_RANGE = "x-emc-range",
		KEY_EMC_SIG = "x-emc-signature",
		KEY_EMC_TAGS = "x-emc-tags",
		KEY_EMC_TOKEN = "x-emc-token",
		KEY_EMC_UID = "x-emc-uid",
		//
		VALUE_KEEP_ALIVE = "keep-alive",
		VALUE_RANGE_PREFIX = "bytes=",
		VALUE_RANGE_CONCAT = "-",
		MSG_TMPL_NOT_SPECIFIED = "Required property \"{}\" is not specifed",
		MSG_NO_DATA_ITEM = "Data item is not specified",
		MSG_NO_REQ = "No request specified to apply to",
		//
		ADAPTER_CLS = "WSRequestConfigImpl",
		// canonicalized EMC headers, should be in alphabetical order
		HEADERS_CANONICAL_EMC[] = {
			KEY_EMC_ACCEPT,
			KEY_EMC_DATE,
			KEY_EMC_FS_ACCESS,
			KEY_EMC_LIMIT,
			KEY_EMC_RANGE,
			KEY_EMC_TAGS,
			KEY_EMC_TOKEN,
			KEY_EMC_UID
		};
	//
	String
		METHOD_PUT = "PUT",
		METHOD_GET = "GET",
		METHOD_POST = "POST",
		METHOD_HEAD = "HEAD",
		METHOD_DELETE = "DELETE",
		METHOD_TRACE = "TRACE";
	//
	HttpEntityEnclosingRequest createDataRequest(final T obj, final String nodeAddr)
	throws URISyntaxException;
	//
	HttpEntityEnclosingRequest createContainerRequest(final C container, final String nodeAddr)
	throws URISyntaxException;
	//
	HttpEntityEnclosingRequest createGenericRequest(final String method, final String uri);
	//
	String getHttpMethod();
	//
	@Override
	HttpRequestConfig<T, C> setAPI(final String api);
	//
	@Override
	HttpRequestConfig<T, C> setLoadType(final IOTask.Type loadType);
	//
	@Override
	HttpRequestConfig<T, C> setUserName(final String userName);
	//
	@Override
	HttpRequestConfig<T, C> setSecret(final String secret);
	//
	@Override
	HttpRequestConfig<T, C> setContentSource(final ContentSource dataSrc);
	//
	@Override
	HttpRequestConfig<T, C> setNameSpace(final String ns);
	//
	@Override
	HttpRequestConfig<T, C> setNamePrefix(final String prefix);
	//
	HttpRequestConfig<T, C> setFileAccessEnabled(final boolean fsAccessFlag);
	boolean getFileAccessEnabled();
	//
	HttpRequestConfig<T, C> setVersioning(final boolean enabledFlag);
	boolean getVersioning();
	//
	HttpRequestConfig<T, C> setPipelining(final boolean enabledFlag);
	boolean getPipelining();
	//
	@Override
	HttpRequestConfig<T, C> setAppConfig(final AppConfig appConfig);
	//
	HeaderGroup getSharedHeaders();
	//
	HttpHost getNodeHost(final String nodeAddr);
	//
	void applyHeadersFinally(final HttpEntityEnclosingRequest httpRequest);
	//
	String getCanonical(final HttpRequest httpRequest);
	//
	String getSignature(final String canonicalForm);
	//
	void applySuccResponseToObject(final HttpResponse response, final T dataItem);
	//
	HttpResponse execute(
		final String addr, final HttpRequest request, final long timeOut, final TimeUnit timeUnit
	);
}
