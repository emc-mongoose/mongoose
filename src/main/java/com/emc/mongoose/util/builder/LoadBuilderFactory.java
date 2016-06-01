package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.client.impl.load.builder.BasicDirectoryLoadBuilderClient;
import com.emc.mongoose.client.impl.load.builder.BasicFileLoadBuilderClient;
import com.emc.mongoose.client.impl.load.builder.BasicHttpContainerLoadBuilderClient;
import com.emc.mongoose.client.impl.load.builder.BasicHttpDataLoadBuilderClient;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.ItemType;
import com.emc.mongoose.common.conf.enums.StorageType;
import com.emc.mongoose.common.conf.Constants;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.builder.BasicDirectoryLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicFileLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicHttpContainerLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicHttpDataLoadBuilder;
import com.emc.mongoose.server.impl.load.builder.BasicDirectoryLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicFileLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicHttpContainerLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicHttpDataLoadBuilderSvc;
//
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {

	@SuppressWarnings("unchecked")
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance (
		final AppConfig appConfig
	) throws ClassNotFoundException, NoSuchMethodException, InstantiationException,
	IllegalAccessException, InvocationTargetException {
		final Class<LoadBuilder<T, U>>
			loadBuilderImplClass = (Class<LoadBuilder<T, U>>) LOAD_BUILDER_MAP
				.get(appConfig.getStorageType())
				.get(appConfig.getItemType())
				.get(appConfig.getRunMode());
		final Constructor<LoadBuilder<T, U>>
			constructor = loadBuilderImplClass.getConstructor(AppConfig.class);
		return constructor.newInstance(appConfig);
	}

	private final static Map<StorageType, Map<ItemType, Map<String, Class<? extends LoadBuilder>>>>
	LOAD_BUILDER_MAP = new HashMap<>();
	static {
		LOAD_BUILDER_MAP.put(
			StorageType.FS,
			new HashMap<ItemType, Map<String, Class<? extends LoadBuilder>>>() {
				{
					put(
						ItemType.CONTAINER,
						new HashMap<String, Class<? extends LoadBuilder>>() {
							{
								put(Constants.RUN_MODE_CLIENT, BasicDirectoryLoadBuilderClient.class);
								put(Constants.RUN_MODE_COMPAT_CLIENT, BasicDirectoryLoadBuilderClient.class);
								put(Constants.RUN_MODE_SERVER, BasicDirectoryLoadBuilderSvc.class);
								put(Constants.RUN_MODE_COMPAT_SERVER, BasicDirectoryLoadBuilderSvc.class);
								put(Constants.RUN_MODE_STANDALONE, BasicDirectoryLoadBuilder.class);
							}
						}
					);
					put(
						ItemType.DATA,
						new HashMap<String, Class<? extends LoadBuilder>>() {
							{
								put(Constants.RUN_MODE_CLIENT, BasicFileLoadBuilderClient.class);
								put(Constants.RUN_MODE_COMPAT_CLIENT, BasicFileLoadBuilderClient.class);
								put(Constants.RUN_MODE_SERVER, BasicFileLoadBuilderSvc.class);
								put(Constants.RUN_MODE_COMPAT_SERVER, BasicFileLoadBuilderSvc.class);
								put(Constants.RUN_MODE_STANDALONE, BasicFileLoadBuilder.class);
							}
						}
					);
				}
			}
		);
		LOAD_BUILDER_MAP.put(
			StorageType.HTTP,
			new HashMap<ItemType, Map<String, Class<? extends LoadBuilder>>>() {
				{
					put(
						ItemType.CONTAINER,
						new HashMap<String, Class<? extends LoadBuilder>>() {
							{
								put(Constants.RUN_MODE_CLIENT, BasicHttpContainerLoadBuilderClient.class);
								put(Constants.RUN_MODE_COMPAT_CLIENT, BasicHttpContainerLoadBuilderClient.class);
								put(Constants.RUN_MODE_SERVER, BasicHttpContainerLoadBuilderSvc.class);
								put(Constants.RUN_MODE_COMPAT_SERVER, BasicHttpContainerLoadBuilderSvc.class);
								put(Constants.RUN_MODE_STANDALONE, BasicHttpContainerLoadBuilder.class);
							}
						}
					);
					put(
						ItemType.DATA,
						new HashMap<String, Class<? extends LoadBuilder>>() {
							{
								put(Constants.RUN_MODE_CLIENT, BasicHttpDataLoadBuilderClient.class);
								put(Constants.RUN_MODE_COMPAT_CLIENT, BasicHttpDataLoadBuilderClient.class);
								put(Constants.RUN_MODE_SERVER, BasicHttpDataLoadBuilderSvc.class);
								put(Constants.RUN_MODE_COMPAT_SERVER, BasicHttpDataLoadBuilderSvc.class);
								put(Constants.RUN_MODE_STANDALONE, BasicHttpDataLoadBuilder.class);
							}
						}
					);
				}
			}
		);
	}
}
