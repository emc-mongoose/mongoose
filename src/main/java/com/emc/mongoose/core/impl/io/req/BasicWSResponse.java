package com.emc.mongoose.core.impl.io.req;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.io.req.MutableWSResponse;
//
import org.apache.http.HttpEntity;
import org.apache.http.ProtocolVersion;
import org.apache.http.ReasonPhraseCatalog;
import org.apache.http.StatusLine;
import org.apache.http.message.AbstractHttpMessage;

import java.util.Locale;
/**
 Created by kurila on 22.05.15.
 */
// TODO
public final class BasicWSResponse
extends AbstractHttpMessage
implements MutableWSResponse {
	@Override
	public ProtocolVersion getProtocolVersion() {
		return null;
	}
	@Override
	public StatusLine getStatusLine() {
		return null;
	}
	//
	@Override
	public void setStatusLine(final StatusLine statusLine) {
	}
	//
	@Override
	public void setStatusLine(final ProtocolVersion ver, final int code) {
	}
	@Override
	public void setStatusLine(final ProtocolVersion ver, final int code, final String reason) {
	}
	//
	@Override
	public void setStatusCode(final int code) throws IllegalStateException {
	}
	@Override
	public void setReasonPhrase(final String reason) throws IllegalStateException {
	}
	@Override
	public HttpEntity getEntity() {
		return null;
	}
	@Override
	public void setEntity(final HttpEntity entity) {
	}
	@Override
	public Locale getLocale() {
		return null;
	}
	@Override
	public void setReasonPhraseCatalog(final ReasonPhraseCatalog catalog) {
	}
	@Override
	public void setLocale(final Locale locale) {
	}
}
