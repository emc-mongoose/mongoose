package com.emc.mongoose.core.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.data.model.DataItemInput;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.load.executor.BasicWSLoadExecutor;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.load.builder.ObjectLoadBuilder;
import com.emc.mongoose.core.api.load.builder.WSLoadBuilder;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.util.NoSuchElementException;
/**
 Created by kurila on 05.05.14.
 */
public class BasicWSLoadBuilder<T extends WSObject, U extends WSLoadExecutor<T>>
extends LoadBuilderBase<T, U>
implements WSLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadBuilder(final RunTimeConfig runTimeConfig) {
		super(runTimeConfig);
		setProperties(runTimeConfig);
	}
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
		final String paramName = RunTimeConfig.KEY_STORAGE_SCHEME;
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
	public final BasicWSLoadBuilder<T, U> clone()
	throws CloneNotSupportedException {
		final BasicWSLoadBuilder<T, U> lb = (BasicWSLoadBuilder<T, U>) super.clone();
		LOG.debug(Markers.MSG, "Cloning request config for {}", reqConf.toString());
		return lb;
	}
	//
	@Override
	protected void invokePreConditions()
	throws IllegalStateException {
		reqConf.configureStorage(dataNodeAddrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		if(reqConf == null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
		final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
		if(minObjSize > maxObjSize) {
			throw new IllegalStateException(
				String.format(
					"Min object size (%s) shouldn't be more than max (%s)",
					SizeUtil.formatSize(minObjSize), SizeUtil.formatSize(maxObjSize)
				)
			);
		}
		//
		final DataItemInput<T> itemSrc = buildItemInput(
			BasicWSObject.class, wsReqConf, dataNodeAddrs, listFile, maxCount,
			minObjSize, maxObjSize, objSizeBias
		);
		//
		return (U) new BasicWSLoadExecutor<>(
			localRunTimeConfig, wsReqConf, dataNodeAddrs,
			threadsPerNodeMap.get(reqConf.getLoadType()), itemSrc,
			maxCount, minObjSize, maxObjSize, objSizeBias, rateLimit, updatesPerItem
		);
	}
}
