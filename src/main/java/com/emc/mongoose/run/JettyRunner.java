package com.emc.mongoose.run;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.TraceLogger;
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
public class JettyRunner {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final RunTimeConfig runTimeConfig;

    public final static String
            webResourceBaseDir,
            webDescriptorBaseDir;

    static {
        webResourceBaseDir = Paths
			.get(Constants.DIR_ROOT, Constants.DIR_WEBAPP)
			.toString();
        webDescriptorBaseDir = Paths
			.get(Constants.DIR_ROOT, Constants.DIR_WEBAPP, Constants.DIR_WEBINF)
			.resolve("web.xml").toString();
    }

    public JettyRunner(RunTimeConfig runTimeConfig) {
        this.runTimeConfig = runTimeConfig;
    }

    public void run() {
        final Server server = new Server(runTimeConfig.getWUISvcPort());
        //
        final WebAppContext webAppContext = new WebAppContext();
        webAppContext.setContextPath("/");
        webAppContext.setResourceBase(webResourceBaseDir);
        webAppContext.setDescriptor(webDescriptorBaseDir);
        webAppContext.setParentLoaderPriority(true);
        webAppContext.setAttribute("runTimeConfig", runTimeConfig);

        //
        server.setHandler(webAppContext);
        //
        try {
            server.start();
            server.join();
        } catch (final Exception e) {
            TraceLogger.failure(LOG, Level.FATAL, e, "Web UI service failure");
        }
    }

}
