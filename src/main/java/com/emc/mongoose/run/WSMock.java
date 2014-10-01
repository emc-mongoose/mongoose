package com.emc.mongoose.run;


import com.emc.mongoose.util.conf.RunTimeConfig;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by olga on 30.09.14.
 */
public final class WSMock {

	public final static void run()
	throws Exception
	{

		final String apiName = RunTimeConfig.getString("storage.api");
		final int port = RunTimeConfig.getInt("api."+apiName+".port");

		// Setup Threadpool
		final QueuedThreadPool threadPool = new QueuedThreadPool(1000000);
		// Setup Jetty Server instance
		final Server server = new Server(threadPool);
		server.manage(threadPool);
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		// Http Connector Setup
		final ServerConnector httpConnector = new ServerConnector(server);
		httpConnector.setPort(port);
		server.addConnector(httpConnector);
		//Set a new handler
		server.setHandler(new SimpleHandler());
		server.start();
		System.out.println("Listening on port "+port);
		server.join();

	}

	@SuppressWarnings("serial")
	public final static class SimpleHandler
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
