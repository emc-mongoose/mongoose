package com.emc.mongoose.web.ui.websockets;

import com.google.gson.Gson;
import org.apache.logging.log4j.core.LogEvent;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;

import java.io.IOException;

/**
 * Created by gusakk on 10/24/14.
 */
@WebSocket
public class LogSocket {

	private static Session _session;

	@OnWebSocketClose
	public void onClose(int statusCode, String reason) {
		System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
	}

	@OnWebSocketError
	public void onError(Throwable t) {
		System.out.println("Error: " + t.getMessage());
	}

	@OnWebSocketConnect
	public void onConnect(Session session) {
		System.out.println("Connect: " + session.getRemoteAddress().getAddress());
		_session = session;
		try {
			session.getRemote().sendString("Hello Webbrowser");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@OnWebSocketMessage
	public void onMessage(String message) {
		System.out.println("Message: " + message);
	}

	public static void sendMessage(LogEvent message) {
		try {
			if (_session != null) {
				_session.getRemote().sendString(new Gson().toJson(message));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
