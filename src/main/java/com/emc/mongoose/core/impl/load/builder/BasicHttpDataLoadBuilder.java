package com.emc.mongoose.core.impl.load.builder;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
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
		final HttpRequestConfig httpReqConf = (HttpRequestConfig) ioConfig;
		//
		return (U) new BasicHttpDataLoadExecutor<>(
			appConfig, httpReqConf, storageNodeAddrs, threadCount,
			itemSrc == null ? getDefaultItemSrc() : itemSrc,
			maxCount, rateLimit
		);
	}
}
