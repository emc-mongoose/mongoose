package com.emc.mongoose.object.api.provider.atmos;
//
import com.emc.mongoose.object.api.WSRequestConfig;
import com.emc.mongoose.object.api.provider.atmos.SubTenant;
import com.emc.mongoose.object.data.WSObject;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpPut;
//
import java.util.Collections;
import java.util.List;
/**
 Created by kurila on 02.10.14.
 */
public class WSSubTenant<T extends WSObject>
implements SubTenant<T> {
	//
	private final WSRequestConfig<T> reqConf;
	private final String name;
	//
	public WSSubTenant(final WSRequestConfig<T> reqConf, final String name) {
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
	public final void create() {
		final HttpRequest createReq = new HttpPut("/"+name);
		
	}
	//
	@Override
	public final void delete() {

	}
	//
	@Override
	public final List<T> enumerate() {
		return Collections.emptyList();
	}
}
