package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.AppConfig.ItemImpl;
import com.emc.mongoose.common.conf.AppConfig.StorageType;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.BasicFileItem;
import com.emc.mongoose.core.impl.item.data.BasicHttpData;
//
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
		BASIC_PREFIX = "Basic",
		LOAD_BUILDER_SUFFIX = "LoadBuilder",
		CLIENT_POSTFIX = "Client";
	//
	@SuppressWarnings("unchecked")
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance(
		final AppConfig appConfig
	) {
		LoadBuilder<T, U> loadBuilderInstance;
		try {
			final Class<LoadBuilder<T, U>>
				loadBuilderImplClass = getLoadBuilderClass(
					appConfig.getRunMode(), appConfig.getItemClass(), appConfig.getStorageClass()
				);
			final Constructor<LoadBuilder<T, U>>
				constructor = loadBuilderImplClass.getConstructor(AppConfig.class);
			loadBuilderInstance = constructor.newInstance(appConfig);
		} catch(final Exception e) {
			e.printStackTrace(System.out);
			throw new RuntimeException(e);
		}
		return loadBuilderInstance;
	}
	//
	@SuppressWarnings("unchecked")
	private static <T extends Item> Class<T> getItemClass(
		final ItemImpl itemImpl, final StorageType storageType
	) {
		if(ItemImpl.CONTAINER.equals(itemImpl)) {
			if(StorageType.FS.equals(storageType)) {
				return (Class<T>) BasicDirectory.class;
			} else { // http
				return (Class<T>) BasicContainer.class;
			}
		} else { // data
			if(StorageType.FS.equals(storageType)) {
				return (Class<T>) BasicFileItem.class;
			} else { // http
				return (Class<T>) BasicHttpData.class;
			}
		}
	}
	//
	@SuppressWarnings("unchecked")
	private static <T extends Item, U extends LoadExecutor<T>> Class<LoadBuilder<T, U>>
	getLoadBuilderClass(
		final String runMode, final ItemImpl itemClass, final StorageType storageType
	) throws ClassNotFoundException {
		Class<LoadBuilder<T, U>> loadBuilderCls = null;
		String
			result = getItemClass(itemClass, storageType).getSimpleName()  + LOAD_BUILDER_SUFFIX,
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
