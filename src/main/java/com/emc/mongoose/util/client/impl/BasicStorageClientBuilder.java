package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
//
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.06.15.
 */
public final class BasicStorageClientBuilder<T extends Item, U extends StorageClient<T>>
implements StorageClientBuilder<T, U> {
	//
	private final RunTimeConfig rtConfig = RunTimeConfig.getContext();
	//
	@Override
	public final StorageClientBuilder<T, U> setAPI(final String api)
	throws IllegalArgumentException {
		if(null == api || 0 == api.length()) {
			throw new IllegalArgumentException("Empty/null storage API specified");
		}
		/*if(null == Package.getPackage(RequestConfig.PACKAGE_IMPL_BASE + "." + api)) {
			throw new IllegalArgumentException(
				"Package \"" + RequestConfig.PACKAGE_IMPL_BASE + "\" doesn't contain a \"" + api +
				"\" storage API implementation sub-package, the package list is: " +
				Arrays.toString(Package.getPackages())
			);
		}*/
		rtConfig.set(RunTimeConfig.KEY_API_NAME, api);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setNodes(final String nodeAddrs[])
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
	public final StorageClientBuilder<T, U> setClientMode(final String loadServers[]) {
		if(loadServers == null || loadServers.length < 1) {
			rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
		} else {
			final StringBuilder addrListBuilder = new StringBuilder();
			for(final String nextAddr : loadServers) {
				if(addrListBuilder.length() > 0) {
					addrListBuilder.append(RunTimeConfig.LIST_SEP);
				}
				addrListBuilder.append(nextAddr);
			}
			rtConfig.set(RunTimeConfig.KEY_LOAD_SERVER_ADDRS, addrListBuilder.toString());
			rtConfig.set(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		}
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setAuth(final String id, final String secret) {
		rtConfig.set(RunTimeConfig.KEY_AUTH_ID, id);
		rtConfig.set(RunTimeConfig.KEY_AUTH_SECRET, secret);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setNamespace(final String value) {
		rtConfig.set(RunTimeConfig.KEY_STORAGE_NAMESPACE, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setS3Bucket(final String value) {
		rtConfig.set(RunTimeConfig.KEY_API_S3_BUCKET, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setSwiftContainer(final String value) {
		rtConfig.set(RunTimeConfig.KEY_API_SWIFT_CONTAINER, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setAtmosSubtenant(final String value) {
		rtConfig.set(RunTimeConfig.KEY_API_ATMOS_SUBTENANT, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setSwiftAuthToken(final String value) {
		rtConfig.set(RunTimeConfig.KEY_API_SWIFT_AUTH_TOKEN, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setLimitCount(final long count)
	throws IllegalArgumentException {
		if(count < 0) {
			throw new IllegalArgumentException("Count limit shouldn' be negative");
		}
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, count);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setLimitTime(
		final long timeOut, final TimeUnit timeUnit
	) throws IllegalArgumentException {
		if(timeOut < 0) {
			throw new IllegalArgumentException("Time limit value shouldn't be negative");
		}
		TimeUnit tu = timeUnit;
		if(timeUnit == null) {
			if(timeOut != 0) {
				throw new IllegalArgumentException("No time limit unit specified");
			} else {
				tu = TimeUnit.SECONDS;
			}
		}
		rtConfig.set(
			RunTimeConfig.KEY_LOAD_LIMIT_TIME,
			Long.toString(timeOut) + tu.name().toLowerCase().charAt(0)
		);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setLimitRate(final float rate)
	throws IllegalArgumentException {
		if(rate < 0) {
			throw new IllegalArgumentException("Rate limit should be >= 0");
		}
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_RATE, rate);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setItemClass(final String itemCls)
	throws IllegalArgumentException {
		rtConfig.set(RunTimeConfig.KEY_ITEM_CLASS, itemCls);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setVersioning(final boolean enabledFlag)
	throws IllegalArgumentException {
		rtConfig.set(RunTimeConfig.KEY_DATA_VERSIONING, enabledFlag);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setFileAccess(final boolean enabledFlag)
	throws IllegalArgumentException {
		rtConfig.set(RunTimeConfig.KEY_DATA_FS_ACCESS, enabledFlag);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setPath(final String path)
	throws IllegalArgumentException {
		if(
			path != null && !path.isEmpty() &&
			(path.charAt(0) == '/' || path.charAt(path.length() - 1) == '/')
		) {
			throw new IllegalArgumentException("Path shouldn't begin or end with \"/\"");
		}
		rtConfig.set(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, path);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setReqThinkTime(final int milliSec) {
		if(milliSec < 0) {
			throw new IllegalArgumentException("Request manual delay should be >= 0");
		}
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_REQSLEEP_MILLISEC, milliSec);
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final U build()
	throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		return (U) new BasicStorageClient<>(
			rtConfig, LoadBuilderFactory.<T, LoadExecutor<T>>getInstance(rtConfig)
		);
	}
}
