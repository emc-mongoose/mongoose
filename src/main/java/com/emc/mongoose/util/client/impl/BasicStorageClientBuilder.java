package com.emc.mongoose.util.client.impl;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.core.api.v1.item.base.Item;
import com.emc.mongoose.core.api.v1.load.executor.LoadExecutor;
//
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
	private final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
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
		appConfig.setProperty(AppConfig.KEY_STORAGE_HTTP_API, api);
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
				addrListBuilder.append(",");
			}
			addrListBuilder.append(nextAddr);
		}
		appConfig.setProperty(AppConfig.KEY_STORAGE_ADDRS, addrListBuilder.toString());
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setClientMode(final String loadServers[]) {
		if(loadServers == null || loadServers.length < 1) {
			appConfig.setProperty(AppConfig.KEY_RUN_MODE, Constants.RUN_MODE_STANDALONE);
		} else {
			final StringBuilder addrListBuilder = new StringBuilder();
			for(final String nextAddr : loadServers) {
				if(addrListBuilder.length() > 0) {
					addrListBuilder.append(",");
				}
				addrListBuilder.append(nextAddr);
			}
			appConfig.setProperty(AppConfig.KEY_LOAD_SERVER_ADDRS, addrListBuilder.toString());
			appConfig.setProperty(AppConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		}
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setAuth(final String id, final String secret) {
		appConfig.setProperty(AppConfig.KEY_AUTH_ID, id);
		appConfig.setProperty(AppConfig.KEY_AUTH_SECRET, secret);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setNamespace(final String value) {
		appConfig.setProperty(AppConfig.KEY_STORAGE_HTTP_NAMESPACE, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setContainer(final String value) {
		appConfig.setProperty(AppConfig.KEY_ITEM_CONTAINER_NAME, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setAuthToken(final String value) {
		appConfig.setProperty(AppConfig.KEY_AUTH_TOKEN, value);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setLimitCount(final long count)
	throws IllegalArgumentException {
		if(count < 0) {
			throw new IllegalArgumentException("Count limit shouldn' be negative");
		}
		appConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_COUNT, count);
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
		appConfig.setProperty(
			AppConfig.KEY_LOAD_LIMIT_TIME,
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
		appConfig.setProperty(AppConfig.KEY_LOAD_LIMIT_RATE, rate);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setItemClass(final String itemCls)
	throws IllegalArgumentException {
		appConfig.setProperty(AppConfig.KEY_ITEM_TYPE, itemCls);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setVersioning(final boolean enabledFlag)
	throws IllegalArgumentException {
		appConfig.setProperty(AppConfig.KEY_STORAGE_HTTP_VERSIONING, enabledFlag);
		return this;
	}
	//
	@Override
	public final StorageClientBuilder<T, U> setFileAccess(final boolean enabledFlag)
	throws IllegalArgumentException {
		appConfig.setProperty(AppConfig.KEY_STORAGE_HTTP_FS_ACCESS, enabledFlag);
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
		appConfig.setProperty(AppConfig.KEY_ITEM_NAMING_PREFIX, path);
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final U build()
	throws ClassNotFoundException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		return (U) new BasicStorageClient<>(
			appConfig, LoadBuilderFactory.<T, LoadExecutor<T>>getInstance(appConfig)
		);
	}
}
