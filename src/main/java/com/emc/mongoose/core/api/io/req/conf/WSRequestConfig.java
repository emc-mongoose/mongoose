package com.emc.mongoose.core.api.io.req.conf;
//
import com.emc.mongoose.core.api.data.src.DataSource;
import com.emc.mongoose.core.api.io.req.MutableWSRequest;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.io.task.WSIOTask;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
//
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.nio.IOControl;
//
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 29.09.14.
 An HTTP request shared configuration.
 */
public interface WSRequestConfig<T extends WSObject>
extends ObjectRequestConfig<T> {
	//
	String
		KEY_EMC_ACCEPT = "x-emc-accept",
		KEY_EMC_FS_ACCESS = "x-emc-file-system-access-enabled",
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
		HEADERS_EMC = {
			KEY_EMC_ACCEPT, KEY_EMC_DATE, /*KEY_EMC_NS, */KEY_EMC_SIG, KEY_EMC_UID, KEY_EMC_FS_ACCESS
		};
	//
	WSIOTask.HTTPMethod getHTTPMethod();
	//
	@Override
	WSRequestConfig<T> setAPI(final String api);
	//
	@Override
	WSRequestConfig<T> setLoadType(final IOTask.Type loadType);
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
	String getNameSpace();
	WSRequestConfig<T> setNameSpace(final String ns);
	//
	WSRequestConfig<T> setFileAccessEnabled(final boolean fsAccessFlag);
	boolean getFileAccessEnabled();
	//
	@Override
	WSRequestConfig<T> setProperties(final RunTimeConfig props);
	//
	@Override
	WSIOTask<T> getRequestFor(final T dataItem, final String nodeAddr)
	throws InterruptedException;
	//
	List<Header> getSharedHeaders();
	//
	Map<String, String> getSharedHeadersMap();
	//
	String getUserAgent();
	//
	void applyDataItem(final MutableWSRequest httpRequest, T dataItem)
	throws URISyntaxException;
	//
	void applyHeadersFinally(final MutableWSRequest httpRequest);
	//
	String getCanonical(final MutableWSRequest httpRequest);
	//
	String getSignature(final String canonicalForm);
	//
	void receiveResponse(final HttpResponse response, final T dataItem);
	//
	boolean consumeContent(final InputStream contentStream, final IOControl ioCtl, T dataItem);
}
