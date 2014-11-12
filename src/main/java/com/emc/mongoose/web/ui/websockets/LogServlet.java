package com.emc.mongoose.web.ui.websockets;



import com.emc.mongoose.web.ui.websockets.LogSocket;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 10/24/14.
 */
public final class LogServlet extends WebSocketServlet {

	@Override
	public final void configure(final WebSocketServletFactory factory) {
		factory.register(LogSocket.class);
	}

}
