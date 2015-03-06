package com.emc.mongoose.webui.websockets.impl;
//
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
/**
 * Created by gusakk on 10/24/14.
 */
public final class LogServlet
extends WebSocketServlet {
	//
	@Override
	public final void configure(final WebSocketServletFactory factory) {
		factory.register(LogSocket.class);
	}
	//
}
