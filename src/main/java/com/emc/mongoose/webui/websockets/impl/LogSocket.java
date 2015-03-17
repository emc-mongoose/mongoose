package com.emc.mongoose.webui.websockets.impl;
//
import com.emc.mongoose.core.impl.util.log.TraceLogger;
import com.emc.mongoose.webui.websockets.WebSocketLogListener;
import com.emc.mongoose.webui.logging.WebUIAppender;
import com.emc.mongoose.core.api.util.log.Markers;
//
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LogEvent;
//
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketException;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
//
import java.io.IOException;

/**
 * Created by gusakk on 10/24/14.
 */
@WebSocket
public final class LogSocket
implements WebSocketLogListener {
	//
	private Session session;
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static Logger LOG = LogManager.getLogger();
	//
	@OnWebSocketClose
	public final void onClose(int statusCode, final String reason) {
		WebUIAppender.unregister(this);
		LOG.trace(Markers.MSG, "Web Socket closed. Reason: {}, StatusCode: {}", reason, statusCode);
	}
	//
	@OnWebSocketError
	public final void onError(final Throwable t) {
		WebUIAppender.unregister(this);
		TraceLogger.failure(LOG, Level.DEBUG, t, "WebSocket failure");
	}
	//
	@OnWebSocketConnect
	public final void onConnect(final Session session) throws InterruptedException {
		this.session = session;
		//
		WebUIAppender.register(this);
		LOG.trace(Markers.MSG, "Web Socket connection {}", session.getRemoteAddress());
	}
	//
	@OnWebSocketMessage
	public final void onMessage(final String message) {
		LOG.trace(Markers.MSG, "Message from Browser {}", message);
	}
	//
	@Override
	public final synchronized void sendMessage(final LogEvent message) {
		try {
			session.getRemote().sendString(mapper.writeValueAsString(message));
		} catch (final IOException|WebSocketException e) {
			TraceLogger.failure(LOG, Level.DEBUG, e, "WebSocket failure");
		}
	}
}
