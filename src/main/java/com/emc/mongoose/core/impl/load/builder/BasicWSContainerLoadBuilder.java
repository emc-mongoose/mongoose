package com.emc.mongoose.core.impl.load.builder;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.conf.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.WSContainerLoadBuilder;
import com.emc.mongoose.core.api.load.executor.WSContainerLoadExecutor;
import com.emc.mongoose.core.impl.io.conf.WSRequestConfigBase;
import com.emc.mongoose.core.impl.load.executor.BasicWSContainerLoadExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.NoSuchElementException;

/**
 * Created by gusakk on 21.10.15.
 */
public class BasicWSContainerLoadBuilder<
	T extends WSObject,
	C extends Container<T>,
	U extends WSContainerLoadExecutor<T, C>
>
extends ContainerLoadBuilderBase<T, C, U>
implements WSContainerLoadBuilder<T, C, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSContainerLoadBuilder(final RunTimeConfig runTimeConfig) {
		super(runTimeConfig);
		setProperties(runTimeConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected WSRequestConfig<T, C> getDefaultRequestConfig() {
		return WSRequestConfigBase.getInstance();
	}
	//
	@Override
	public BasicWSContainerLoadBuilder<T, C, U> setProperties(final RunTimeConfig rtConfig) {
		//
		super.setProperties(rtConfig);
		//
		final String paramName = RunTimeConfig.KEY_STORAGE_SCHEME;
		try {
			WSRequestConfig.class.cast(ioConfig).setScheme(rtConfig.getStorageProto());
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
	public final BasicWSContainerLoadBuilder<T, C, U> clone()
	throws CloneNotSupportedException {
		final BasicWSContainerLoadBuilder<T, C, U> lb
			= (BasicWSContainerLoadBuilder<T, C, U>) super.clone();
		LOG.debug(Markers.MSG, "Cloning request config for {}", ioConfig.toString());
		return lb;
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		//  do nothing
		//  ioConfig.configureStorage(storageNodeAddrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		if(ioConfig == null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(ioConfig);
		final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
		//
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int
			connPerNode = loadTypeConnPerNode.get(loadType),
			minThreadCount = getMinIOThreadCount(
				loadTypeWorkerCount.get(loadType), storageNodeAddrs.length, connPerNode
			);
		//
		return (U) new BasicWSContainerLoadExecutor<>(
			localRunTimeConfig, wsReqConf, storageNodeAddrs, connPerNode, minThreadCount,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, manualTaskSleepMicroSecs, rateLimit
		);
	}
}
