package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
//
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.06.15.
 */
public abstract class BasicClientBuilder<T extends WSObject, U extends StorageClient<T>>
implements StorageClientBuilder<T, U> {
	//
	private final RunTimeConfig rtConfig = RunTimeConfig.getContext();
	//
	@Override
	public StorageClientBuilder<T, U> setAPI(final String api)
	throws IllegalArgumentException {
		if(null == api || 0 == api.length()) {
			throw new IllegalArgumentException("Empty/null storage API specified");
		}
		if(null == Package.getPackage(RequestConfig.PACKAGE_IMPL_BASE + "." + api)) {
			throw new IllegalArgumentException(
				"Package \"" + RequestConfig.PACKAGE_IMPL_BASE + "\" doesn't contain a \"" + api +
				"\" storage API implementation sub-package"
			);
		}
		rtConfig.set(RunTimeConfig.KEY_API_NAME, api);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setNodes(final String nodeAddrs[])
	throws IllegalArgumentException {
		if(nodeAddrs == null || nodeAddrs.length < 1) {
			throw new IllegalArgumentException("Empty/null node address list specified");
		}
		final StringBuilder addrListBuilder = new StringBuilder();
		for(final String nextAddr : nodeAddrs) {
			if(addrListBuilder.length() > 0) {
				addrListBuilder.append(RunTimeConfig.LIST_SEP);
			}
			addrListBuilder.append(nextAddr);
		}
		rtConfig.set(RunTimeConfig.KEY_STORAGE_ADDRS, addrListBuilder.toString());
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setClientMode(final String loadServers[])
	throws IllegalArgumentException {
		if(loadServers == null || loadServers.length < 1) {
			throw new IllegalArgumentException("Empty/null load server address list specified");
		}
		final StringBuilder addrListBuilder = new StringBuilder();
		for(final String nextAddr : loadServers) {
			if(addrListBuilder.length() > 0) {
				addrListBuilder.append(RunTimeConfig.LIST_SEP);
			}
			addrListBuilder.append(nextAddr);
		}
		rtConfig.set(RunTimeConfig.KEY_STORAGE_ADDRS, addrListBuilder.toString());
		rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setAuth(final String id, final String secret)
	throws IllegalArgumentException {
		if(id != null) {
			rtConfig.set(RunTimeConfig.KEY_AUTH_ID, id);
		}
		if(secret != null) {
			rtConfig.set(RunTimeConfig.KEY_AUTH_SECRET, secret);
		}
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setS3Bucket(final String value)
	throws IllegalArgumentException {
		if(value == null || value.length() == 0) {
			throw new IllegalArgumentException("Empty/null S3 bucket name specified");
		}
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, value);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setSwiftContainer(final String value) {
		if(value == null || value.length() == 0) {
			throw new IllegalArgumentException("Empty/null Swift container specified");
		}
		rtConfig.set(RunTimeConfig.KEY_API_SWIFT_CONTAINER, value);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setAtmosSubtenant(final String value) {
		if(value == null || value.length() == 0) {
			throw new IllegalArgumentException("Empty/null Atmos subtenant specified");
		}
		rtConfig.set(RunTimeConfig.KEY_API_ATMOS_SUBTENANT, value);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setSwiftAuthToken(final String value) {
		if(value == null || value.length() == 0) {
			throw new IllegalArgumentException("Empty/null Swift auth token specified");
		}
		rtConfig.set(RunTimeConfig.KEY_API_SWIFT_AUTH_TOKEN, value);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setLimitCount(final long count)
	throws IllegalArgumentException {
		if(count < 1) {
			throw new IllegalArgumentException("Count limit should be a positive integer");
		}
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, count);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setLimitTime(final long timeOut, final TimeUnit timeUnit)
	throws IllegalArgumentException {
		if(timeOut < 1) {
			throw new IllegalArgumentException("Time limit value should be a positive integer");
		}
		if(timeUnit == null) {
			throw new IllegalArgumentException("No time limit unit specified");
		}
		rtConfig.set(
			RunTimeConfig.KEY_LOAD_LIMIT_TIME,
			Long.toString(timeOut) + timeUnit.name().toLowerCase().charAt(0)
		);
		return this;
	}
	//
	@Override
	public StorageClientBuilder<T, U> setLimitRate(final float rate)
	throws IllegalArgumentException {

		return this;
	}
}
