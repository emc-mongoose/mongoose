package com.emc.mongoose.storage.client.impl;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
//
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 16.06.15.
 */
public class BasicWSClient<T extends WSObject>
extends BasicStorageClient<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@Override
	public BasicWSClient<T> api(final String name)
	throws IllegalStateException {
		try {
			Class.forName(
				RequestConfig.PACKAGE_IMPL_BASE + "." + name + "." + WSRequestConfig.ADAPTER_CLS
			);
		} catch(final ClassNotFoundException | NullPointerException e) {
			throw new IllegalStateException(e);
		}
		super.api(name);
		return this;
	}
}
