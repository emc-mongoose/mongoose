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
import com.emc.mongoose.client.impl.load.builder.BasicWSContainerLoadBuilderClient;
//
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	@SuppressWarnings("unchecked")
	public static <T extends Item, U extends LoadExecutor<T>> LoadBuilder<T, U> getInstance(
		final RunTimeConfig rtConfig
	) {
		final String mode = rtConfig.getRunMode();
		final String itemClassName = rtConfig.getLoadItemClass();
		//
		LoadBuilder loadBuilderInstance = null;
		try {
			switch(mode) {
				case Constants.RUN_MODE_CLIENT:
				case Constants.RUN_MODE_COMPAT_CLIENT:
					switch(itemClassName) {
						case Constants.LOAD_ITEMS_CLASS_OBJECT:
							loadBuilderInstance = new BasicWSDataLoadBuilderClient(rtConfig);
							break;
						case Constants.LOAD_ITEMS_CLASS_CONTAINER:
							loadBuilderInstance = new BasicWSContainerLoadBuilderClient(rtConfig);
							break;
						default:
							throw new IllegalArgumentException(
								"Failed to recognize the load item class \"" + itemClassName + "\""
							);
					}
				case Constants.RUN_MODE_SERVER:
				case Constants.RUN_MODE_COMPAT_SERVER:
					loadBuilderInstance = new BasicWSDataLoadBuilderSvc(rtConfig);
					break;
				case Constants.RUN_MODE_CINDERELLA:
				case Constants.RUN_MODE_WEBUI:
				case Constants.RUN_MODE_WSMOCK:
					throw new IllegalArgumentException(
						"Load builder shouldn't be invoked from the run mode \"" + mode + "\""
					);
				default:
					switch(itemClassName) {
						case Constants.LOAD_ITEMS_CLASS_OBJECT:
							loadBuilderInstance = new BasicWSDataLoadBuilder(rtConfig);
							break;
						case Constants.LOAD_ITEMS_CLASS_CONTAINER:
							loadBuilderInstance = new BasicWSContainerLoadBuilder(rtConfig);
							break;
						default:
							throw new IllegalArgumentException(
								"Failed to recognize the load item class \"" + itemClassName + "\""
							);
					}
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the load builder");
		}
		return loadBuilderInstance;
	}
}
