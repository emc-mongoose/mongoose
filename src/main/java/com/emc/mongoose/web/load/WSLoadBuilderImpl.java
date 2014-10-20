package com.emc.mongoose.web.load;
//
import com.emc.mongoose.base.load.LoadBuilderBase;
import com.emc.mongoose.object.load.ObjectLoadBuilder;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.api.WSRequestConfigBase;
import com.emc.mongoose.web.data.WSObjectImpl;
import com.emc.mongoose.web.load.type.AppendImpl;
import com.emc.mongoose.web.load.type.CreateImpl;
import com.emc.mongoose.web.load.type.DeleteImpl;
import com.emc.mongoose.web.load.type.ReadImpl;
import com.emc.mongoose.web.load.type.UpdateImpl;
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
public class WSLoadBuilderImpl<T extends WSObjectImpl, U extends WSLoadExecutor<T>>
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
	public final WSLoadBuilderImpl<T, U> clone()
	throws CloneNotSupportedException {
		final WSLoadBuilderImpl<T, U> lb = (WSLoadBuilderImpl<T, U>) super.clone();
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
		if(minObjSize <= maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						LOG.debug("New create load");
						load = new CreateImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						load = new ReadImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						load = new UpdateImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						load = new DeleteImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						load = new AppendImpl<T>(
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
