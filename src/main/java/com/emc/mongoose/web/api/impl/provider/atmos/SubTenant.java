package com.emc.mongoose.web.api.impl.provider.atmos;
//
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObject;
//
import java.util.Collections;
import java.util.List;
/**
 Created by kurila on 02.10.14.
 */
public class SubTenant<T extends WSObject>
implements com.emc.mongoose.object.api.provider.atmos.SubTenant<T> {
	//
	@SuppressWarnings("FieldCanBeLocal")
	private final WSRequestConfig<T> reqConf;
	private final String name;
	//
	public SubTenant(final WSRequestConfig<T> reqConf, final String name) {
		this.reqConf = reqConf;
		this.name = name;
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final boolean exists()
	throws IllegalStateException {
		return true;
	}
	//
	@Override
	public final void create()
	throws IllegalStateException {
		// TODO
	}
	//
	@Override
	public final void delete()
	throws IllegalStateException {
		// TODO
	}
	//
	@Override
	public final List<T> list()
	throws IllegalStateException {
		return Collections.emptyList();
	}
	//
}
