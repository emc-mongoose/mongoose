package com.emc.mongoose.run;

import com.emc.mongoose.conf.RunTimeConfig;
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
public class WSMock {

	public static void run() throws Exception
	{

		final String apiName = RunTimeConfig.getString("storage.api");
		final int port = RunTimeConfig.getInt("api."+apiName+".port");

		// Setup Threadpool
		QueuedThreadPool threadPool = new QueuedThreadPool(512);
		// Setup Jetty Server instance
		Server server = new Server(threadPool);
		server.manage(threadPool);
		server.setDumpAfterStart(false);
		server.setDumpBeforeStop(false);
		// Http Connector Setup

		// A plain HTTP connector listening on port 8080. Note that it's also possible to have port 8080 configured as
		// a non SSL SPDY connector. But the specification and most browsers do not allow to use SPDY without SSL
		// encryption. However some browsers allow it to be configured.
		//HttpConnectionFactory http = new HttpConnectionFactory();
		ServerConnector httpConnector = new ServerConnector(server);

		httpConnector.setPort(port);
		//httpConnector.setIdleTimeout(10000);
		server.addConnector(httpConnector);
		//Set a new handler
		server.setHandler(new SimpleHandler());

		// Start things up! By using the server.join() the server thread will join with the current thread.
		// See "http://docs.oracle.com/javase/1.5.0/docs/api/java/lang/Thread.html#join()" for more details.
		server.start();
		System.out.println("Listening on port "+port);
		server.join();

	}

	@SuppressWarnings("serial")
	public static class SimpleHandler extends AbstractHandler {


		@Override
		public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
			response.setStatus(HttpServletResponse.SC_OK);
			System.out.println("<< Response code 200_OK");
			baseRequest.setHandled(true);
		}
	}
}
