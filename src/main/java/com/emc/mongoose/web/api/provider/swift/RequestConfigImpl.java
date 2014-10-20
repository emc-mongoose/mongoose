package com.emc.mongoose.web.api.provider.swift;
//
import com.emc.mongoose.web.api.WSRequestConfigBase;
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
public final class RequestConfigImpl<T extends WSObject>
extends WSRequestConfigBase<T> {
	//
	public RequestConfigImpl()
	throws NoSuchAlgorithmException {
		super();
		api = RequestConfigImpl.class.getSimpleName();
	}
	//
	@Override @SuppressWarnings("UnnecessaryLocalVariable")
	public RequestConfigImpl<T> clone()
	throws CloneNotSupportedException {
		final RequestConfigImpl copy = (RequestConfigImpl<T>) super.clone();
		// TODO add swift specific fields
		return copy;
	}
	//
	@Override
	public RequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
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
