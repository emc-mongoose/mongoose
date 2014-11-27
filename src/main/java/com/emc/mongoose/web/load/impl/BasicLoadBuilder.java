package com.emc.mongoose.web.load.impl;
//
import com.emc.mongoose.base.load.impl.LoadBuilderBase;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.api.impl.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.WSLoadBuilder;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.web.load.impl.type.Append;
import com.emc.mongoose.web.load.impl.type.Create;
import com.emc.mongoose.web.load.impl.type.Delete;
import com.emc.mongoose.web.load.impl.type.Read;
import com.emc.mongoose.web.load.impl.type.Update;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 05.05.14.
 */
public class BasicLoadBuilder<T extends WSObject, U extends WSLoadExecutor<T>>
extends LoadBuilderBase<T, U>
implements WSLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig<T> getDefaultRequestConfig() {
		return (WSRequestConfig<T>) WSRequestConfigBase.getInstance();
	}
	//
	@Override
	public ObjectLoadBuilder<T, U> setProperties(final RunTimeConfig runTimeConfig) {
		//
		super.setProperties(runTimeConfig);
		//
		final String paramName = "storage.scheme";
		try {
			WSRequestConfig.class.cast(reqConf).setScheme(runTimeConfig.getStorageProto());
		} catch(final NoSuchElementException e) {
			LOG.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			LOG.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public final BasicLoadBuilder<T, U> clone()
	throws CloneNotSupportedException {
		final BasicLoadBuilder<T, U> lb = (BasicLoadBuilder<T, U>) super.clone();
		LOG.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
		return lb;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public U build()
	throws IllegalStateException {
		if(reqConf == null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		try {
			reqConf
				.setAddr(dataNodeAddrs[0])
				.configureStorage();
		} catch(final NullPointerException | IndexOutOfBoundsException | IllegalStateException e) {
			throw new IllegalStateException(e);
		}
		//
		WSLoadExecutor<T> load = null;
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
		final RunTimeConfig localRunTimeConfig = Main.RUN_TIME_CONFIG.get();
		if(minObjSize <= maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						LOG.debug(Markers.MSG, "New create load");
						load = new Create<T>(
							localRunTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize, objSizeBias
						);
						break;
					case READ:
						LOG.debug(Markers.MSG, "New read load");
						load = new Read<T>(
							localRunTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case UPDATE:
						LOG.debug(Markers.MSG, "New update load");
						load = new Update<T>(
							localRunTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug(Markers.MSG, "New delete load");
						load = new Delete<T>(
							localRunTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case APPEND:
						LOG.debug(Markers.MSG, "New append load");
						load = new Append<T>(
							localRunTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
				}
				// cleanup after a specific load executor constructor takes the client
				wsReqConf.setClient(null); // please don't touch
			} catch(CloneNotSupportedException | IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			throw new IllegalStateException(
				"Min object size ("+Long.toString(minObjSize)+
				") should be less than upper bound "+Long.toString(maxObjSize)
			);
		}
		//
		return (U) load;
	}
}
