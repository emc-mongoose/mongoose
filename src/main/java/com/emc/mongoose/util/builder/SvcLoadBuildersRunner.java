package com.emc.mongoose.util.builder;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSContainerLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by gusakk on 23.10.15.
 */
public class SvcLoadBuildersRunner {
	//
	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger();
	//
	public static void startSvcLoadBuilders(final List<LoadBuilderSvc> services) {
		//
		try {
			for (final LoadBuilderSvc service : services) {
				service.start();
			}
			//
			for (final LoadBuilderSvc service : services) {
				service.await();
			}
		} catch (final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Load builder service failure");
		} catch (final InterruptedException e) {
			LOG.debug(Markers.MSG, "Interrupted load builder service");
		}
	}
	//
	public static List<LoadBuilderSvc> getSvcBuilders(final RunTimeConfig rtConfig) {
		final List<LoadBuilderSvc> builders = new ArrayList<>();
		builders.add(new BasicWSContainerLoadBuilderSvc(rtConfig));
		builders.add(new BasicWSDataLoadBuilderSvc(rtConfig));
		//
		return builders;
	}
}


