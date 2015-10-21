package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.builder.BasicWSContainerLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicWSDataLoadBuilder;
//
import com.emc.mongoose.client.impl.load.builder.BasicWSDataLoadBuilderClient;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static <T extends Item, U extends LoadExecutor<T>, V extends LoadBuilder<T, U>> V getInstance(
		final RunTimeConfig rtConfig
	) {
		final String mode = rtConfig.getRunMode();
		final String itemClassName = rtConfig.getLoadItemClass();
		//
		LoadBuilder loadBuilderInstance = null;
		switch(mode) {
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				try {
					loadBuilderInstance = (LoadBuilder) new BasicWSDataLoadBuilderClient<>(rtConfig);
				} catch(final IOException | NoSuchElementException | ClassCastException e) {
					LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the load builder");
				}
				break;
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				loadBuilderInstance = (LoadBuilder) new BasicWSDataLoadBuilderSvc<>(rtConfig);
				break;
			default:
				switch (itemClassName) {
					case Constants.LOAD_ITEMS_CLASS_OBJECT:
						loadBuilderInstance = (LoadBuilder) new BasicWSDataLoadBuilder<>(rtConfig);
						break;
					case Constants.LOAD_ITEMS_CLASS_CONTAINER:
						break;
				}
		}
		return (V) loadBuilderInstance;
	}
}
