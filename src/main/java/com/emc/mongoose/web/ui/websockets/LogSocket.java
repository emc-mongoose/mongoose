package com.emc.mongoose.web.ui.websockets;

import com.emc.mongoose.util.logging.CustomAppender;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.ui.websockets.interfaces.WebSocketLogListener;
import com.google.gson.Gson;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;

/**
 * Created by gusakk on 10/24/14.
 */
@WebSocket
public class LogSocket implements WebSocketLogListener {

	private Session session;
	private final static Gson gson = new Gson();
	private final static Logger LOG = LogManager.getLogger();

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		CustomAppender.unregister(this);
		LOG.info(Markers.MSG, "Web Socket closed. Reason: {}, StatusCode: {}", reason, statusCode);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		CustomAppender.unregister(this);
		LOG.info(Markers.ERR, "Web Socket error. Message: {}", t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		LOG.info(Markers.MSG, "Web Socket connection {}", session.getRemoteAddress());
		this.session = session;
		CustomAppender.register(this);
		for (LogEvent logEvent : CustomAppender.getLogEventsList()) {
			sendMessage(logEvent);
		}
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		LOG.info(Markers.MSG, "Message from JS {}", message);
	}

	public synchronized void sendMessage(LogEvent message) {
		try {
			session.getRemote().sendString(gson.toJson(message));
		} catch (IOException e) {
			ExceptionHandler.trace(LOG, Level.ERROR, e, "WebSocket problem");
		}
	}


}
