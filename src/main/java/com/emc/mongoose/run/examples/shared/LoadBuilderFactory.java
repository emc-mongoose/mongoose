package com.emc.mongoose.run.examples.shared;
//
import com.emc.mongoose.client.impl.load.builder.BasicWSLoadBuilderClient;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
//
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.core.impl.load.builder.BasicWSLoadBuilder;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.util.NoSuchElementException;
/**
 Created by kurila on 09.06.15.
 */
public class LoadBuilderFactory {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public static LoadBuilder getInstance() {
		final RunTimeConfig runTimeConfig = RunTimeConfig.getContext();
		final String mode = runTimeConfig.getRunMode();
		LoadBuilder loadBuilderInstance = null;
		switch(mode) {
			case Constants.RUN_MODE_CLIENT:
			case Constants.RUN_MODE_COMPAT_CLIENT:
				try {
					loadBuilderInstance = new BasicWSLoadBuilderClient(runTimeConfig);
				} catch(final IOException | NoSuchElementException e) {
					LogUtil.exception(LOG, Level.FATAL, e, "Failed to create the load builder");
				}
				break;
			default:
				loadBuilderInstance = new BasicWSLoadBuilder(runTimeConfig);
		}
		return loadBuilderInstance;
	}
}
