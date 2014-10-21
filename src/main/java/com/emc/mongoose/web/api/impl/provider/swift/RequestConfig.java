package com.emc.mongoose.web.api.impl.provider.swift;
//
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.security.NoSuchAlgorithmException;
//
/**
 Created by kurila on 26.03.14.
 */
public final class RequestConfig<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	public RequestConfig()
	throws NoSuchAlgorithmException {
		super();
		api = RequestConfig.class.getSimpleName();
	}
	//
	@Override @SuppressWarnings("UnnecessaryLocalVariable")
	public RequestConfig<T> clone()
	throws CloneNotSupportedException {
		final RequestConfig copy = (RequestConfig<T>) super.clone();
		// TODO add swift specific fields
		return copy;
	}
	//
	@Override
	public RequestConfig<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		// TODO swift specific things
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
		throws IOException, ClassNotFoundException {
		super.readExternal(in);
		// TODO add swift specific fields
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
		throws IOException {
		super.writeExternal(out);
		// TODO add swift specific fields
	}
	//
	@Override
	protected final void applyURI(final HttpRequest httpRequest, final WSObject dataItem) {
		// TODO swift specific things
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequest httpRequest) {
		// TODO swift specific things
	}
	//
	@Override
	public final String getCanonical(final HttpRequest httpRequest) {
		// TODO swift specific things
		return null;
	}
	//
	@Override
	public final void applyObjectId(final T dataObject, final HttpResponse httpResponse) {
		// TODO swift specific things
	}
}
