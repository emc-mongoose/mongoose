package com.emc.mongoose.web.ui.websockets;

import com.emc.mongoose.util.logging.CustomAppender;
import com.emc.mongoose.util.logging.ExceptionHandler;
import com.emc.mongoose.util.logging.Markers;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;

/**
 * Created by gusakk on 10/31/14.
 */
@WebSocket
public class CookieSocket {

	private final static Logger LOG = LogManager.getLogger();

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		LOG.info(Markers.MSG, "Web Socket closed. Reason: {}, StatusCode: {}", reason, statusCode);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		LOG.info(Markers.ERR, "Web Socket error. Message: {}", t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		LOG.info(Markers.MSG, "Web Socket connection {}", session.getRemoteAddress());
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		LOG.info(Markers.MSG, "Message from JS {}", message);
	}

}
