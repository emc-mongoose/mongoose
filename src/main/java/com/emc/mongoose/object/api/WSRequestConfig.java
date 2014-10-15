package com.emc.mongoose.object.api;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.data.DataSource;
import com.emc.mongoose.object.data.WSObject;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.impl.client.CloseableHttpClient;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
/**
 Created by kurila on 29.09.14.
 An HTTP request shared configuration.
 */
public interface WSRequestConfig<T extends WSObject>
extends ObjectRequestConfig<T> {
	//
	String
		DEFAULT_ENC = StandardCharsets.UTF_8.name(),
		//
		KEY_EMC_ACCEPT = "x-emc-accept",
		KEY_EMC_DATE = "x-emc-date",
		KEY_EMC_NS = "x-emc-namespace",
		KEY_EMC_RANGE = "x-emc-range",
		KEY_EMC_SIG = "x-emc-signature",
		KEY_EMC_UID = "x-emc-uid",
		//
		VALUE_KEEP_ALIVE = "keep-alive",
		MSG_TMPL_NOT_SPECIFIED = "Required property \"{}\" is not specifed",
		MSG_TMPL_RANGE_BYTES = "bytes=%d-%d",
		MSG_TMPL_RANGE_BYTES_APPEND = "bytes=%d-",
		MSG_NO_DATA_ITEM = "Data item is not specified",
		MSG_NO_REQ = "No request specified to apply to";
		//
	String[]
		HEADERS4CANONICAL = {
			HttpHeaders.CONTENT_MD5, HttpHeaders.CONTENT_TYPE, HttpHeaders.DATE
		},
		HEADERS_EMC = {
			KEY_EMC_ACCEPT, KEY_EMC_DATE, KEY_EMC_NS, KEY_EMC_SIG, KEY_EMC_UID
		};
	//
	DateFormat FMT_DT = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.ROOT);
	//
	@Override
	WSRequestConfig<T> setAPI(final String api);
	//
	@Override
	WSRequestConfig<T> setAddr(final String addr);
	//
	@Override
	WSRequestConfig<T> setLoadType(final Request.Type loadType);
	//
	@Override
	WSRequestConfig<T> setPort(final int port);
	//
	@Override
	WSRequestConfig<T> setUserName(final String userName);
	//
	@Override
	WSRequestConfig<T> setSecret(final String secret);
	//
	@Override
	WSRequestConfig<T> setDataSource(final DataSource<T> dataSrc);
	//
	@Override
	WSRequestConfig<T> setRetries(final boolean retryFlag);
	//
	@Override
	WSRequestConfig<T> setProperties(final RunTimeConfig props);
	//
	@Override
	WSRequestConfig<T> clone()
	throws CloneNotSupportedException;
	//
	String getScheme();
	WSRequestConfig<T> setScheme(final String scheme);
	//
	CloseableHttpClient getClient();
	WSRequestConfig<T> setClient(final CloseableHttpClient httpClient);
	//
	Map<String, String> getSharedHeadersMap();
	//
	String getUserAgent();
	//
	void applyDataItem(final HttpRequest httpRequest, T dataItem)
	throws URISyntaxException;
	//
	void applyHeadersFinally(final HttpRequest httpRequest);
	//
	HttpRequestRetryHandler getRetryHandler();
	//
	String getCanonical(final HttpRequest httpRequest);
	//
	String getSignature(final String canonicalForm);
}
