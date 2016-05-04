package com.emc.mongoose.run.webserver;
//
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.nio.file.Paths;
//
//
//
/**
 * Created by gusakk on 02/10/14.
 */
public class WebUiRunner
		implements Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private static final String
			WEB_RESOURCE_BASE_DIR,
			WEB_DESCRIPTOR_BASE_DIR;
	//
	static {
		WEB_RESOURCE_BASE_DIR = Paths
				.get(BasicConfig.getRootDir(), Constants.DIR_WEBAPP)
				.toString();
		WEB_DESCRIPTOR_BASE_DIR = Paths
				.get(BasicConfig.getRootDir(), Constants.DIR_WEBAPP, Constants.DIR_WEBINF)
				.resolve("web.xml").toString();
	}
	//
	@Override
	public void run() {
		final Server server = new Server(8080);
		//
		final WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("/");
		webAppContext.setResourceBase(WEB_RESOURCE_BASE_DIR);
		webAppContext.setDescriptor(WEB_DESCRIPTOR_BASE_DIR);
		webAppContext.setParentLoaderPriority(true);
		//
		server.setHandler(webAppContext);
		try {
			server.start();
			server.join();
		} catch (final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Web UI service failure");
		}
	}
}