package com.emc.mongoose.object.api.impl.provider.http;
//
import com.emc.mongoose.object.api.impl.WSRequestConfigBase;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.object.data.WSDataObject;
//
import org.apache.http.client.methods.HttpRequestBase;
//
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
//
/**
 Created by kurila on 26.03.14.
 */
public final class Swift
extends WSRequestConfigBase<WSDataObject> {
	//
	public Swift() {
		api = Swift.class.getSimpleName();
	}
	//
	@Override
	public Swift clone() {
		final Swift copy = new Swift();
		copy.setAddr(getAddr());
		copy.setLoadType(getLoadType());
		copy.setPort(getPort());
		copy.setUserName(getUserName());
		copy.setSecret(getSecret());
		copy.setScheme(getScheme());
		copy.setClient(getClient());
		copy.setNameSpace(getNameSpace()); // ?
		// TODO add swift specific fields
		return copy;
	}
	//
	@Override
	public Swift setProperties(final RunTimeConfig props) {
		super.setProperties(props);
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
	protected final void applyURI(final HttpRequestBase httpRequest, final WSDataObject dataItem) {
		// TODO swift specific things
	}
	//
	@Override
	protected final void applyAuthHeader(final HttpRequestBase httpRequest) {
		// TODO swift specific things
	}

}
