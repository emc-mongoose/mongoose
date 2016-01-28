package com.emc.mongoose.webui.websockets;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.log.appenders.WebSocketLogListener;
import com.emc.mongoose.common.log.appenders.WebUIAppender;
//
import com.emc.mongoose.core.impl.load.tasks.LogMetricsTask;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
//
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.*;

@WebSocket
public final class CommonWebSocketListener implements WebSocketLogListener {
	//
	private Session session;
	private final static ObjectMapper mapper = new ObjectMapper();
	private final static Logger LOG = LogManager.getLogger();

	static {
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
	}
	//
	@OnWebSocketClose
	public final void onClose(int statusCode, final String reason) {
		unregisterListener();
		LOG.trace(Markers.MSG, "Web Socket closed. Reason: {}, StatusCode: {}", reason, statusCode);
	}
	//
	@OnWebSocketError
	public final void onError(final Throwable t) {
		unregisterListener();
		LogUtil.exception(LOG, Level.DEBUG, t, "WebSocket failure");
	}
	//
	@OnWebSocketConnect
	public final void onConnect(final Session session) throws InterruptedException {
		this.session = session;
		registerListener();
		LOG.trace(Markers.MSG, "Web Socket connection {}", session.getRemoteAddress());
	}
	//
	@OnWebSocketMessage
	public final void onMessage(final String message) {
		LOG.trace(Markers.MSG, "Message from Browser {}", message);
	}
	//
	@Override
	public final void sendMessage(final Object message) {
		if(session.isOpen()) {
			try {
				session.getRemote().sendString(mapper.writeValueAsString(message));
			} catch (final IOException e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "WebSocket failure");
			}
		}
	}

	private void registerListener() {
		WebUIAppender.register(this);
		LogMetricsTask.setListeners(WebUIAppender.listeners());
	}

	private void unregisterListener() {
		WebUIAppender.unregister(this);
		session.close();
		LogMetricsTask.setListeners(WebUIAppender.listeners());
	}

}
