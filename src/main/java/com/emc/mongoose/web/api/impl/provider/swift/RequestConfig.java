package com.emc.mongoose.web.api.impl.provider.swift;
//
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	private Logger log = LogManager.getLogger(new MessageFactoryImpl(Main.RUN_TIME_CONFIG));
	//
	public RequestConfig()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected RequestConfig(final RequestConfig<T> reqConf2Clone)
	throws NoSuchAlgorithmException {
		super(reqConf2Clone);
		api = RequestConfig.class.getSimpleName();
		if(reqConf2Clone != null) {
			// TODO copy swift specific fields
		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public RequestConfig<T> clone() {
		RequestConfig<T> copy = null;
		try {
			copy = new RequestConfig<>(this);
		} catch(final NoSuchAlgorithmException e) {
			log.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		}
		return copy;
	}
	//
	@Override
	public RequestConfig<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
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
