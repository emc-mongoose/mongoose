package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
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
		LoadBuilder loadBuilderInstance;
		try {
			final Class loadBuilderImplClass = getLoadBuilderClass(
				rtConfig.getRunMode(), rtConfig.getLoadItemClass()
			);
			final Constructor constructor = loadBuilderImplClass.getConstructor(RunTimeConfig.class);
			loadBuilderInstance = (LoadBuilder) constructor.newInstance(rtConfig);
		} catch(final Exception e) {
			e.printStackTrace(System.out);
			throw new RuntimeException(e);
		}
		return loadBuilderInstance;
	}
	//
	@SuppressWarnings("unchecked")
	private static Class<LoadBuilder> getLoadBuilderClass(
			final String runMode, final String itemClass
	) throws ClassNotFoundException {
		String itemClassName = BASIC_PREFIX + WordUtils.capitalize(itemClass) + LOAD_BUILDER_POSTFIX;
		final String itemClassFQN;
		// don't append anything if run.mode is standalone
		switch(runMode) {
			case Constants.RUN_MODE_COMPAT_CLIENT:
			case Constants.RUN_MODE_CLIENT:
				itemClassName = itemClassName + CLIENT_POSTFIX;
				itemClassFQN = BUILDER_CLIENT_PACKAGE_BASE + "." + itemClassName;
				break;
			case Constants.RUN_MODE_STANDALONE:
				itemClassFQN = BUILDER_CORE_PACKAGE_BASE + "." + itemClassName;
				break;
			case Constants.RUN_MODE_COMPAT_SERVER:
			case Constants.RUN_MODE_SERVER:
				itemClassFQN = MultiLoadBuilderSvc.class.getCanonicalName();
				break;
			default:
				throw new IllegalArgumentException(
					"Failed to recognize the run mode \"" + runMode + "\""
				);
		}
		//
		Class<LoadBuilder> loadBuilderCls;
		try {
			loadBuilderCls = (Class<LoadBuilder>) Class.forName(itemClassFQN);
		} catch(final ClassNotFoundException e) {
			loadBuilderCls = (Class<LoadBuilder>) Class.forName(WS_PREFIX + itemClassFQN);
		}
		return loadBuilderCls;
	}
}
