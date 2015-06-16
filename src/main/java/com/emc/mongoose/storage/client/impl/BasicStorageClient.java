package com.emc.mongoose.storage.client.impl;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.io.req.conf.RequestConfig;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.storage.client.api.StorageClient;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.06.15.
 */
public class BasicStorageClient<T extends DataItem>
implements StorageClient<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected final RunTimeConfig rtConfig = RunTimeConfig.getContext();
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> api(final String api)
	throws IllegalArgumentException {
		if(api == null || api.length() == 0) {
			throw new IllegalArgumentException("Empty storage API specified");
		}
		if(null == Package.getPackage(RequestConfig.PACKAGE_IMPL_BASE + "." + api)) {
			throw new IllegalArgumentException(
				"Storage API implementation was not found for \"" + api + "\""
			);
		}
		rtConfig.setProperty(RunTimeConfig.KEY_API_NAME, api);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> nodes(final String[] nodeAddrs)
	throws IllegalArgumentException {
		if(nodeAddrs == null || nodeAddrs.length == 0) {
			throw new IllegalArgumentException("Empty storage node address list specified");
		}
		rtConfig.setProperty(RunTimeConfig.KEY_STORAGE_ADDRS, nodeAddrs);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> remote(final String[] loadServers)
	throws IllegalArgumentException {
		if(loadServers == null || loadServers.length == 0) {
			throw new IllegalArgumentException("Empty storage node address list specified");
		}
		rtConfig.setProperty(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_CLIENT);
		rtConfig.setProperty(RunTimeConfig.KEY_LOAD_SERVERS, loadServers);
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> auth(final String id, final String secret) {
		if(id == null || id.length() == 0) {
			throw new IllegalArgumentException("Empty user id specified");
		}
		if(secret == null || secret.length() == 0) {
			throw new IllegalArgumentException("Empty secret specified");
		}
		rtConfig.setProperty(RunTimeConfig.KEY_AUTH_ID, id);
		rtConfig.setProperty(RunTimeConfig.KEY_AUTH_SECRET, secret);
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> bucket(final String value) {
		rtConfig.setProperty(RunTimeConfig.KEY_API_S3_BUCKET, value);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> container(final String value) {
		rtConfig.setProperty(RunTimeConfig.KEY_SWIFT_CONTAINER, value);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> subtenant(final String value) {
		rtConfig.setProperty(RunTimeConfig.KEY_ATMOS_SUBTENANT, value);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> authToken(final String value) {
		rtConfig.setProperty(RunTimeConfig.KEY_SWIFT_AUTH_TOKEN, value);
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> limitCount(final long count) {
		rtConfig.setProperty(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, count);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> limitTime(
		final long timeOut, final TimeUnit timeUnit
	) {
		rtConfig.setProperty(
			RunTimeConfig.KEY_LOAD_LIMIT_TIME, timeOut + timeUnit.name().toLowerCase().charAt(0)
		);
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> limitRate(final float rate) {
		rtConfig.setProperty(RunTimeConfig.KEY_LOAD_LIMIT_RATE, rate);
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> write(final long size) {
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> write(
		final long minSize, final long maxSize, final float sizeBias
	) {
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> read() {
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> read(final boolean verifyContent) {
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> delete() {
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> update() {
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> update(final int countPerTime) {
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public synchronized BasicStorageClient<T> append() {
		return this;
	}
	//
	@Override
	public synchronized BasicStorageClient<T> append(final long augmentSize) {
		return this;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
}
