package com.emc.mongoose.storage.adapter.sdk;
//

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.logging.Markers;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.load.model.Producer;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.io.req.conf.RequestConfigBase;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.util.NoSuchElementException;

//
//
//
//
//

/**
 Created by kurila on 26.03.14.
 */
public final class RequestConfigImpl<T extends DataObject>
extends RequestConfigBase<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public final static String
		KEY_POOL_NAME = "api.type.sdk.Pool",
		MSG_NO_POOL = "Pool is not specified",
		FMT_MSG_ERR_POOL_NOT_EXIST = "Created pool \"%s\" still doesn't exist";
//	private final String authPrefixValue;
	//
	private PoolImpl<T> pool;
	//
	public RequestConfigImpl()
	throws NoSuchAlgorithmException {
		this(null);
	}
	//
	protected RequestConfigImpl(final RequestConfigImpl<T> reqConf2Clone)
	throws NoSuchAlgorithmException{
		super(reqConf2Clone);
//		authPrefixValue = runTimeConfig.getApiS3AuthPrefix() + " ";
//		if(reqConf2Clone != null) {
//			setPool(reqConf2Clone.getPool());
//			setNameSpace(reqConf2Clone.getNameSpace());
//		}
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public RequestConfigImpl<T> clone() {
		RequestConfigImpl<T> copy = null;
		//try {
			//copy = new RequestConfigImpl<>(this);
		//} catch(final NoSuchAlgorithmException e) {
			//LOG.fatal(Markers.ERR, "No such algorithm: \"{}\"", signMethod);
		//}
		return copy;
	}
	//
//	public final PoolImpl<T> getPool() {
//		return pool;
//	}
	//
	public final RequestConfigImpl<T> setPool(final PoolImpl<T> pool) {
		LOG.debug(Markers.MSG, "Req conf instance #{}: set pool \"{}\"", hashCode(), pool);
		this.pool = pool;
		return this;
	}

	@Override
	public final RequestConfigImpl<T> setProperties(final RunTimeConfig runTimeConfig) {
		super.setProperties(runTimeConfig);
		//
		try {
			final PoolImpl<T> pool = new PoolImpl<>(
				this, this.runTimeConfig.getString(KEY_POOL_NAME),
				this.runTimeConfig.getStorageVersioningEnabled()
			);
			setPool(pool);
		} catch(final NoSuchElementException e) {
			//LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, KEY_POOL_NAME);
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		final String poolName = String.class.cast(ObjectInputStream.class.cast(in).readUnshared());
		LOG.debug(Markers.MSG, "Note: pool {} has been got from load client side", poolName);
		setPool(new PoolImpl<>(this, poolName, runTimeConfig.getStorageVersioningEnabled()));
	}
	//
	@Override
	public final void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		ObjectOutputStream.class.cast(out).writeUnshared(pool.getName());
	}
	//

	@Override @SuppressWarnings("unchecked")
	public final Producer<T> getAnyDataProducer(final long maxCount, final String addr) {
		Producer<T> producer = null;
		if(anyDataProducerEnabled) {
			try {
				producer = new PoolProducer<>(pool, BasicWSObject.class, maxCount, addr);
			} catch(final NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Unexpected failure");
			}
		} else {
			LOG.debug(
				Markers.MSG, "req conf {}: using of pool listing data producer is suppressed",
				hashCode()
			);
		}
		return producer;
	}
	//
	@Override
	public final void configureStorage(final String[] storageNodeAddrs)
	throws IllegalStateException {
		if(pool == null) {
			throw new IllegalStateException("Pool is not specified");
		} else {
			LOG.debug(Markers.MSG, "Configure storage w/ pool \"{}\"", pool);
		}
		final String poolName = pool.getName();
		if(pool.exists(storageNodeAddrs[0])) {
			LOG.info(Markers.MSG, "Pool \"{}\" already exists", poolName);
		} else {
			LOG.debug(Markers.MSG, "Pool \"{}\" doesn't exist, trying to create", poolName);
			pool.create(storageNodeAddrs[0]);
			if(pool.exists(storageNodeAddrs[0])) {
				runTimeConfig.set(KEY_POOL_NAME, poolName);
			} else {
				throw new IllegalStateException(
					String.format(FMT_MSG_ERR_POOL_NOT_EXIST, poolName)
				);
			}
		}
	}
}
