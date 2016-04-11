package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.ItemType;
import com.emc.mongoose.common.conf.enums.StorageType;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.item.ItemTypeUtil;
//
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {
	//
	private final static String
		BUILDER_CORE_PACKAGE_BASE = "com.emc.mongoose.core.impl.v1.load.builder",
		BUILDER_CLIENT_PACKAGE_BASE = "com.emc.mongoose.client.impl.load.builder",
		LOAD_BUILDER_SUFFIX = "LoadBuilder",
		CLIENT_POSTFIX = "Client";
	//
	@SuppressWarnings("unchecked")
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance(
		final AppConfig appConfig
	) throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
	IllegalAccessException, InvocationTargetException {
		final Class<LoadBuilder<T, U>>
			loadBuilderImplClass = getLoadBuilderClass(
				appConfig.getRunMode(), appConfig.getItemType(), appConfig.getStorageType()
			);
		final Constructor<LoadBuilder<T, U>>
			constructor = loadBuilderImplClass.getConstructor(AppConfig.class);
		return constructor.newInstance(appConfig);
	}
	//
	@SuppressWarnings("unchecked")
	private static <T extends Item, U extends LoadExecutor<T>> Class<LoadBuilder<T, U>>
	getLoadBuilderClass(
		final String runMode, final ItemType itemClass, final StorageType storageType
	) throws ClassNotFoundException {
		Class<LoadBuilder<T, U>> loadBuilderCls = null;
		String
			result = ItemTypeUtil.getItemClass(itemClass, storageType).getSimpleName() +
				LOAD_BUILDER_SUFFIX,
			itemClassPackage = null;
		// don't append anything if run.mode is standalone
		switch(runMode) {
			case Constants.RUN_MODE_COMPAT_CLIENT:
			case Constants.RUN_MODE_CLIENT:
				result = result + CLIENT_POSTFIX;
				itemClassPackage = BUILDER_CLIENT_PACKAGE_BASE;
				break;
			case Constants.RUN_MODE_STANDALONE:
				itemClassPackage = BUILDER_CORE_PACKAGE_BASE;
				break;
			case Constants.RUN_MODE_COMPAT_SERVER:
			case Constants.RUN_MODE_SERVER:
				loadBuilderCls = (Class) MultiLoadBuilderSvc.class;
				break;
			default:
				throw new IllegalArgumentException(
					"Failed to recognize the run mode \"" + runMode + "\""
				);
		}
		//
		if(loadBuilderCls == null) {
			loadBuilderCls = (Class<LoadBuilder<T, U>>) Class.<LoadBuilder<T, U>>forName(
				itemClassPackage + "." + result
			);
		}
		return loadBuilderCls;
	}
}
