package com.emc.mongoose.util.factory;
//
import com.emc.mongoose.client.impl.load.builder.BasicWSLoadBuilderClient;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.Item;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.builder.WSLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicWSLoadBuilder;
import com.emc.mongoose.server.impl.load.builder.BasicWSLoadBuilderSvc;
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
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance(
		final RunTimeConfig rtConfig
	) {
		return newInstanceFor(rtConfig);
	}
	//
	@SuppressWarnings({"unchecked", "ConstantConditions"})
	private static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> newInstanceFor(
		final RunTimeConfig rtConfig
	) {
		final String mode = rtConfig.getRunMode();
		//
		LoadBuilder<T, U> loadBuilderInstance = null;
		switch(mode) {
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				try {
					loadBuilderInstance = (WSLoadBuilder) new BasicWSLoadBuilderClient<>(rtConfig);
				} catch(final IOException | NoSuchElementException | ClassCastException e) {
					LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the load builder");
				}
				break;
			case Constants.RUN_MODE_SERVER:
			case Constants.RUN_MODE_COMPAT_SERVER:
				loadBuilderInstance = (WSLoadBuilder) new BasicWSLoadBuilderSvc<>(rtConfig);
				break;
			default:
				loadBuilderInstance = (WSLoadBuilder) new BasicWSLoadBuilder<>(rtConfig);
		}
		return loadBuilderInstance;
	}
}
