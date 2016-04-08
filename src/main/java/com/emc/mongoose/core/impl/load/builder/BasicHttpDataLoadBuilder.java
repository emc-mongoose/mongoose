package com.emc.mongoose.core.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
// mongoose-core-impl.jar
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.load.executor.BasicHttpDataLoadExecutor;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.load.builder.HttpDataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.HttpDataLoadExecutor;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.load.executor.BasicMixedHttpDataLoadExecutor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 05.05.14.
 */
public class BasicHttpDataLoadBuilder<T extends HttpDataItem, U extends HttpDataLoadExecutor<T>>
extends DataLoadBuilderBase<T, U>
implements HttpDataLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpDataLoadBuilder(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
		setAppConfig(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected HttpRequestConfig<T, ? extends Container<T>> getDefaultIoConfig() {
		return HttpRequestConfigBase.getInstance();
	}
	//
	@Override @SuppressWarnings("CloneDoesntCallSuperClone")
	public final BasicHttpDataLoadBuilder<T, U> clone()
	throws CloneNotSupportedException {
		final BasicHttpDataLoadBuilder<T, U> lb = (BasicHttpDataLoadBuilder<T, U>) super.clone();
		LOG.debug(Markers.MSG, "Cloning request config for {}", ioConfig.toString());
		return lb;
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		((HttpRequestConfig) ioConfig).configureStorage(storageNodeAddrs);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		if(ioConfig == null) {
			throw new IllegalStateException("No I/O configuration instance available");
		}
		//
		final LoadType loadType = ioConfig.getLoadType();
		final HttpRequestConfig httpReqConf = (HttpRequestConfig) ioConfig;
		if(LoadType.MIXED.equals(loadType)) {
			final Map<LoadType, Integer> loadTypeWeightMap = LoadType.getMixedLoadWeights(
				(List<String>) appConfig.getProperty(AppConfig.KEY_LOAD_TYPE)
			);
			final Map<LoadType, Input<T>> itemInputMap = new HashMap<>();
			for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
				try {
					itemInputMap.put(
						nextLoadType,
						LoadType.WRITE.equals(nextLoadType) ? getNewItemInput() : itemInput
					);
				} catch(final NoSuchMethodException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to build new item src");
				}
			}
			return (U) new BasicMixedHttpDataLoadExecutor<>(
				appConfig, httpReqConf, storageNodeAddrs, threadCount,
				maxCount, rateLimit, sizeConfig, rangesConfig,
				loadTypeWeightMap, itemInputMap
			);
		} else {
			return (U) new BasicHttpDataLoadExecutor<>(
				appConfig, httpReqConf, storageNodeAddrs, threadCount,
				itemInput == null ? getDefaultItemInput() : itemInput,
				maxCount, rateLimit, sizeConfig, rangesConfig
			);
		}
	}
}
