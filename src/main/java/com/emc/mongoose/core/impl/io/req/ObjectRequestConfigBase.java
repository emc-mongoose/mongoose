package com.emc.mongoose.core.impl.io.req;
// mongoose-core-api.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.req.ObjectRequestConfig;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
/**
 Created by kurila on 23.12.14.
 */
public abstract class ObjectRequestConfigBase<T extends DataObject>
extends RequestConfigBase<T>
implements ObjectRequestConfig<T> {
	//
	protected volatile String idPrefix = null;
	//
	protected ObjectRequestConfigBase(final ObjectRequestConfig<T> reqConf2Clone) {
		super(reqConf2Clone);
		if(reqConf2Clone != null) {
			setIdPrefix(reqConf2Clone.getIdPrefix());
		}
	}
	//
	@Override
	public ObjectRequestConfigBase<T> setProperties(final RunTimeConfig rtConfig) {
		if(rtConfig != null) {
			setIdPrefix(rtConfig.getDataPrefix());
			super.setProperties(rtConfig);
		}
		return this;
	}
	//
	@Override
	public String getIdPrefix() {
		return idPrefix;
	}
	//
	@Override
	public ObjectRequestConfigBase<T> setIdPrefix(final String idPrefix) {
		this.idPrefix = idPrefix;
		return this;
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		setIdPrefix(String.class.cast(in.readObject()));
	}
	//
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeObject(getIdPrefix());
	}
}
