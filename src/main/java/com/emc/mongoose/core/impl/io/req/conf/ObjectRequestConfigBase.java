package com.emc.mongoose.core.impl.io.req.conf;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.io.req.conf.ObjectRequestConfig;
//
import com.emc.mongoose.core.api.load.executor.ObjectLoadExecutor;
import com.emc.mongoose.core.impl.io.task.BasicObjectIOTask;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;



/**
 Created by kurila on 23.12.14.
 */

public abstract class ObjectRequestConfigBase<T extends DataObject>
extends RequestConfigBase<T>
implements ObjectRequestConfig<T> {
	//

	private final static Logger LOG = LogManager.getLogger();

	protected ObjectRequestConfigBase(final ObjectRequestConfig<T> reqConf2Clone) {
		super(reqConf2Clone);
	}

	public static RequestConfigBase getInstance() {
		return newInstanceFor(RunTimeConfig.getContext().getApiName());
	}

	private final static String
			FMT_CLS_PATH_ADAPTER_IMPL = "com.emc.mongoose.storage.adapter.%s.ObjectRequestConfigImpl";
	public static RequestConfigBase newInstanceFor(final String api) {
		RequestConfigBase reqConf = null;
		final String apiImplClsFQN = String.format(FMT_CLS_PATH_ADAPTER_IMPL, api.toLowerCase());
		try {
			final Class apiImplCls = Class.forName(apiImplClsFQN);
			final Constructor<RequestConfigBase>
					constructor = (Constructor<RequestConfigBase>) apiImplCls.getConstructors()[0];
			reqConf = constructor.newInstance();
		} catch(final ClassNotFoundException e) {
			LogUtil.exception(LOG, Level.FATAL, e, "API implementation \"{}\" is not found", api);
		} catch(final ClassCastException e) {
			LogUtil.exception(
					LOG, Level.FATAL, e,
					"Class \"{}\" is not valid API implementation for \"{}\"", apiImplClsFQN, api
			);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, " API config instantiation failure");
		}
		return reqConf;
	}

	/*@Override
	public Producer<T> getAnyDataProducer(long maxCount, LoadExecutor<T> loadExecutor) {
		return null;
	}
	//
	@Override
	public void configureStorage(LoadExecutor<T> loadExecutor) throws IllegalStateException {
	}*/
}
