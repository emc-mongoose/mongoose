package com.emc.mongoose.run;
//
import com.emc.mongoose.util.conf.RunTimeConfig;
//
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
//
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
/**
 * Created by olga on 30.09.14.
 */
public final class WSMock
implements Runnable {

	private final static Logger LOG = LogManager.getLogger();
	private final RunTimeConfig runTimeConfig;

	public WSMock(final RunTimeConfig runTimeConfig) {
		this.runTimeConfig = runTimeConfig;
	}

	public void run() {
		final String apiName = runTimeConfig.getStorageApi();
		final int port = runTimeConfig.getInt("api."+apiName+".port");
		// Setup Jetty Server instance
		final Server server = new Server();
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		// Http Connector Setup
		final ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(port);
		server.addConnector(httpConnector);
		//Set a new handler
		server.setHandler(new SimpleHandler());
		try {
            server.start();
            LOG.info(Markers.MSG, "Listening on port #{}", port);
            server.join();
        } catch (final Exception e) {
            ExceptionHandler.trace(LOG, Level.WARN, e, "WSMock was interrupted");
        } finally {
            try {
                server.stop();
            } catch (final Exception e) {
                ExceptionHandler.trace(LOG, Level.WARN, e, "Failed to stop jetty");
            }
        }
	}

	@SuppressWarnings("serial")
	private final static class SimpleHandler
	extends AbstractHandler {

		@Override
		public final void handle(
			final String target, final Request baseRequest, final HttpServletRequest request,
			final HttpServletResponse response
		) throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_OK);
			baseRequest.setHandled(true);
		}
	}
}
