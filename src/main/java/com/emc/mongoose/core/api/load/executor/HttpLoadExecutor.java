package com.emc.mongoose.core.api.load.executor;
//
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
//
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 12.04.16.
 */
public interface HttpLoadExecutor<T extends Item, A extends IoTask<T>>
extends LoadExecutor<T, A>, HttpRequestInterceptor {
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
		ADAPTER_CLS = "HttpRequestConfigImpl",
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
		},
		SCHEME = "http",
		HOST_PORT_SEP = ":";
	//
	String
		METHOD_PUT = "PUT",
		METHOD_GET = "GET",
		METHOD_POST = "POST",
		METHOD_HEAD = "HEAD",
		METHOD_DELETE = "DELETE",
		METHOD_TRACE = "TRACE";
	//
	HttpHost getNodeHost(final String nodeAddr);
	//
	void applyHeadersFinally(final HttpRequest httpRequest);
	//
	HttpEntityEnclosingRequest createRequest(final A ioTask);
	//
	HttpEntityEnclosingRequest createGenericRequest(final String method, final String uri);
	//
	HttpResponse execute(
		final String addr, final HttpRequest request, final long timeOut, final TimeUnit timeUnit
	);
}
