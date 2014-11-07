package com.emc.mongoose.web.load.impl;
//
import com.emc.mongoose.base.load.impl.LoadBuilderBase;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.logging.MessageFactoryImpl;
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
	protected Logger log = LogManager.getLogger(new MessageFactoryImpl(Main.RUN_TIME_CONFIG));
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
		log = LogManager.getLogger(new MessageFactoryImpl(runTimeConfig));
		//
		final String paramName = "storage.scheme";
		try {
			WSRequestConfig.class.cast(reqConf).setScheme(runTimeConfig.getStorageProto());
		} catch(final NoSuchElementException e) {
			log.error(Markers.ERR, MSG_TMPL_NOT_SPECIFIED, paramName);
		} catch(final IllegalArgumentException e) {
			log.error(Markers.ERR, MSG_TMPL_INVALID_VALUE, paramName, e.getMessage());
		}
		//
		return this;
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public final BasicLoadBuilder<T, U> clone()
	throws CloneNotSupportedException {
		final BasicLoadBuilder<T, U> lb = (BasicLoadBuilder<T, U>) super.clone();
		log.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
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
		if(minObjSize <= maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						log.debug(Markers.MSG, "New create load");
						load = new Create<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
						break;
					case READ:
						log.debug(Markers.MSG, "New read load");
						load = new Read<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case UPDATE:
						log.debug(Markers.MSG, "New update load");
						load = new Update<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, updatesPerItem
						);
						break;
					case DELETE:
						log.debug(Markers.MSG, "New delete load");
						load = new Delete<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case APPEND:
						log.debug(Markers.MSG, "New append load");
						load = new Append<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
				}
			} catch(CloneNotSupportedException|IOException e) {
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
