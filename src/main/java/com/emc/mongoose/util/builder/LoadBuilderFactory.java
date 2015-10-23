package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.builder.WSDataLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
//
//
//
import com.emc.mongoose.server.api.load.builder.WSContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.builder.WSDataLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSContainerLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.lang.reflect.Constructor;

/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		BUILDER_CORE_PACKAGE_BASE = "com.emc.mongoose.core.impl.load.builder",
		BUILDER_CLIENT_PACKAGE_BASE = "com.emc.mongoose.client.impl.load.builder",
		WS_PREFIX = "WS",
		BASIC_PREFIX = "Basic",
		LOAD_BUILDER_POSTFIX = "LoadBuilder",
		CLIENT_POSTFIX = "Client";
	//
	@SuppressWarnings("unchecked")
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance(
		final RunTimeConfig rtConfig
	) {
		final String runMode = rtConfig.getRunMode();
		final String apiName = rtConfig.getApiName();
		//
		String itemClassName = WordUtils.capitalize(rtConfig.getLoadItemClass());
		//
		if (
			apiName.equals(Constants.API_TYPE_S3) ||
			apiName.equals(Constants.API_TYPE_ATMOS) ||
			apiName.equals(Constants.API_TYPE_SWIFT)
		) {
			itemClassName = WS_PREFIX + itemClassName;
		}
		//
		itemClassName = BASIC_PREFIX + itemClassName + LOAD_BUILDER_POSTFIX;
		final String itemClassFQN;
		//  don't append anything if run.mode is standalone
		switch(runMode) {
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				itemClassName = itemClassName + CLIENT_POSTFIX;
				itemClassFQN = BUILDER_CLIENT_PACKAGE_BASE + "." + itemClassName;
				break;
			case Constants.RUN_MODE_STANDALONE:
				itemClassFQN = BUILDER_CORE_PACKAGE_BASE + "." + itemClassName;
				break;
			default:
				throw new IllegalArgumentException(
					"Failed to recognize the run mode \"" + runMode + "\""
				);
		}
		//
		LoadBuilder loadBuilderInstance;
		try {
			final Class loadBuilderImplClass = Class.forName(itemClassFQN);
			final Constructor constructor = loadBuilderImplClass.getConstructor(RunTimeConfig.class);
			loadBuilderInstance = (LoadBuilder) constructor.newInstance(rtConfig);
		} catch(final Exception e) {
			e.printStackTrace(System.out);
			throw new RuntimeException(e);
		}
		return loadBuilderInstance;
	}
	//
	public static void startSvcBuilders(final RunTimeConfig rtConfig) {
		try {
			final WSDataLoadBuilderSvc<WSObject, WSDataLoadSvc<WSObject>>
				loadBuilderSvc = new BasicWSDataLoadBuilderSvc<>(rtConfig);
			final WSContainerLoadBuilderSvc containerBuilderSvc =
				new BasicWSContainerLoadBuilderSvc(rtConfig);
			//
			loadBuilderSvc.start();
			containerBuilderSvc.start();
			//
			loadBuilderSvc.await();
			containerBuilderSvc.await();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}
}
