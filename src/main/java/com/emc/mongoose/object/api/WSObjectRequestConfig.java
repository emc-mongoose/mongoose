package com.emc.mongoose.object.api;
//
import com.emc.mongoose.object.data.WSDataObject;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
/**
 Created by kurila on 29.09.14.
 */
public interface WSObjectRequestConfig<T extends WSDataObject>
extends DataObjectRequestConfig<T> {
	//
	String
		DEFAULT_ENC = StandardCharsets.UTF_8.name(),
		DEFAULT_USERAGENT = RunTimeConfig.getString("run.name") + '/' +
			RunTimeConfig.getString("run.version"),
		//
		KEY_EMC_ACCEPT = "x-emc-accept",
		KEY_EMC_DATE = "x-emc-date",
		KEY_EMC_NS = "x-emc-namespace",
		KEY_EMC_RANGE = "x-emc-range",
		KEY_EMC_SIG = "x-emc-signature",
		KEY_EMC_UID = "x-emc-uid",
		//
		VALUE_SIGN_METHOD = RunTimeConfig.getString("http.sign.method"),
		REQ_DATA_TYPE = RunTimeConfig.getString("http.content.type"),
		VALUE_KEEP_ALIVE = "keep-alive",
		MSG_TMPL_NOT_SPECIFIED = "Required property \"{}\" is not specifed",
		MSG_TMPL_RANGE_BYTES = "bytes=%d-%d",
		MSG_TMPL_RANGE_BYTES_APPEND = "bytes=%d-",
		MSG_NO_DATA_ITEM = "Data item is not specified",
		MSG_NO_REQ = "No request specified to apply to",
		//
		REL_PKG_WS_PROVIDERS = ".provider.http.";
	String[]
		HEADERS4CANONICAL = {
			HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE
		},
		HEADERS_EMC = {
			KEY_EMC_ACCEPT, KEY_EMC_DATE, KEY_EMC_NS, KEY_EMC_SIG, KEY_EMC_UID
		};
	//
	String getScheme();
	WSObjectRequestConfig<T> setScheme(final String scheme);
	//
	CloseableHttpClient getClient();
	WSObjectRequestConfig<T> setClient(final CloseableHttpClient httpClient);
	//
	Map<String, String> getSharedHeadersMap();
	//
	void applyDataItem(final HttpRequestBase httpRequest, T dataItem)
	throws URISyntaxException;
	//
	void applyHeadersFinally(final HttpRequestBase httpRequest);
	//
	HttpRequestRetryHandler getRetryHandler();
}
