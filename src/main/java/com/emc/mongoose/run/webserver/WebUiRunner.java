package com.emc.mongoose.run.webserver;

import com.emc.mongoose.common.log.LogUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Paths;

import static com.emc.mongoose.common.conf.BasicConfig.getWorkingDir;
import static com.emc.mongoose.common.conf.Constants.DIR_WEBAPP;
import static com.emc.mongoose.common.conf.Constants.DIR_WEBINF;

/**
 * Created by gusakk on 02/10/14.
 */
public class WebUiRunner
implements Closeable, Runnable {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	private static final String
			WEB_RESOURCE_BASE_DIR,
			WEB_DESCRIPTOR_BASE_DIR;
	//
	static {
		WEB_RESOURCE_BASE_DIR = Paths
			.get(getWorkingDir(), DIR_WEBAPP).toString();
		WEB_DESCRIPTOR_BASE_DIR = Paths
			.get(getWorkingDir(), DIR_WEBAPP, DIR_WEBINF).resolve("web.xml").toString();
	}

	private final Server server;
	private final WebAppContext webAppContext;

	public WebUiRunner() {
		server = new Server(8080);
		webAppContext = new WebAppContext();
		webAppContext.setContextPath("/");
		webAppContext.setResourceBase(WEB_RESOURCE_BASE_DIR);
		webAppContext.setDescriptor(WEB_DESCRIPTOR_BASE_DIR);
		webAppContext.setParentLoaderPriority(true);
		server.setHandler(webAppContext);
	}

	@Override
	public final void run() {
		try {
			server.start();
			server.join();
		} catch(final InterruptedException ignored) {
		} catch (final Exception e) {
			LogUtil.exception(LOG, Level.FATAL, e, "Web UI service failure");
		}
	}

	@Override
	public final void close()
	throws IOException {
		try {
			webAppContext.stop();
			webAppContext.destroy();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Close failed");
		}
		try {
			server.stop();
			server.destroy();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Close failed");
		}
	}
}
