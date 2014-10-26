package com.emc.mongoose.web.ui.websockets;

import com.emc.mongoose.util.logging.CustomAppender;
import com.google.gson.Gson;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;
import java.util.EventListener;

/**
 * Created by gusakk on 10/24/14.
 */
@WebSocket
public class LogSocket implements WebSocketLogListener {

	private Session session;

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		CustomAppender.unregister(this);
		System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		CustomAppender.unregister(this);
		System.out.println("Error: " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("Connect: " + session.getRemoteAddress().getAddress());
		this.session = session;
		try {
			session.getRemote().sendString("Hello Webbrowser");
			CustomAppender.register(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		System.out.println("Message: " + message);
	}

	public void sendMessage(LogEvent message) {
		try {
			session.getRemote().sendString(new Gson().toJson(message));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
