package com.emc.mongoose.core.api.io.req;
//
import org.apache.http.HttpResponse;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;
//
import java.util.Locale;
/**
 Created by kurila on 22.05.15.
 */
public interface MutableWSResponse
extends HttpResponse {
	//
	void setStatusLine(final StatusLine statusLine);
	//
	void setReasonPhraseCatalog(final ReasonPhraseCatalog catalog);
	//
	void setLocale(final Locale locale);
}
