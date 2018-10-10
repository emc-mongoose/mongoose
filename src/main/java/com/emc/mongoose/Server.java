package com.emc.mongoose;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;
import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 @author veronika K. on 05.10.18 */
public class Server extends AsyncRunnableBase {

	private final org.eclipse.jetty.server.Server server;

	public Server(final int port) {
		server = new org.eclipse.jetty.server.Server(port);
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
	}

	public void doStart() {
		try {
			server.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	public void doStop() {
		try {
			server.stop();
			server.join();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
