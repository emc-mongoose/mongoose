package com.emc.mongoose.run.webserver;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
//
import java.nio.file.Paths;
/**
 * Created by gusakk on 02/10/14.
 */
public class WUIRunner
implements Runnable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final AppConfig appConfig;
	//
	public final static String
			webResourceBaseDir,
			webDescriptorBaseDir;
	//
	static {
		webResourceBaseDir = Paths
			.get(BasicConfig.getRootDir(), Constants.DIR_WEBAPP)
			.toString();
		webDescriptorBaseDir = Paths
			.get(BasicConfig.getRootDir(), Constants.DIR_WEBAPP, Constants.DIR_WEBINF)
			.resolve("web.xml").toString();
	}
	//
	public WUIRunner(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
	//
	@Override
	public void run() {
		final Server server = new Server(8080);
		//
		final WebAppContext webAppContext = new WebAppContext();
		webAppContext.setContextPath("/");
		webAppContext.setResourceBase(webResourceBaseDir);
		webAppContext.setDescriptor(webDescriptorBaseDir);
		webAppContext.setParentLoaderPriority(true);
		webAppContext.setAttribute("config", appConfig);
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
