package com.emc.mongoose;

import io.prometheus.client.exporter.MetricsServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import java.util.Arrays;

/**
 @author veronika K. on 05.10.18 */
public class Server {

	final org.eclipse.jetty.server.Server server;

	public Server() {
		this(1234);  //default port
	}

	public Server(final int port) {
		server = new org.eclipse.jetty.server.Server(port);
		ServletContextHandler context = new ServletContextHandler();
		context.setContextPath("/");
		server.setHandler(context);
		context.addServlet(new ServletHolder(new MetricsServlet()), "/metrics");
	}

	public Server start() {
		try {
			server.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
		return this;
	}

	public void stop() {
		try {
			server.stop();
			Arrays.asList(server.getChildHandlers()).stream().peek(handler -> {
				try {
					handler.stop();
				} catch(Exception e) {
					e.printStackTrace();
				}
			});
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
