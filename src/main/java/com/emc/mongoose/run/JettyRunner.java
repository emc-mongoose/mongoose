package com.emc.mongoose.run;

import com.emc.mongoose.util.logging.ExceptionHandler;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.nio.file.Paths;

/**
 * Created by gusakk on 02/10/14.
 */
public class JettyRunner {

    private final static Logger LOG = LogManager.getLogger();

    public final static int JETTY_PORT = 8080;

    public final static String
        DIR_WEBAPP = "webapp",
        DIR_WEBINF = "WEB-INF";

    public final static String
            webResourceBaseDir,
            webDescriptorBaseDir;

    static {
        webResourceBaseDir = Paths
			.get(Main.DIR_ROOT, DIR_WEBAPP)
			.toString();
        webDescriptorBaseDir = Paths
			.get(Main.DIR_ROOT, DIR_WEBAPP, DIR_WEBINF)
			.resolve("web.xml").toString();
    }

    public static void run() {
        final Server server = new Server(JETTY_PORT);
        //
        final WebAppContext webAppContext = new WebAppContext();
        webAppContext.setResourceBase(webResourceBaseDir);
        webAppContext.setDescriptor(webDescriptorBaseDir);
        webAppContext.setParentLoaderPriority(true);
        //
        server.setHandler(webAppContext);
        //
        try {
            server.start();
            server.join();
        } catch (final Exception e) {
            ExceptionHandler.trace(LOG, Level.FATAL, e, "Web UI service failure");
        }
    }

}
