package ca.redleafsolutions.ishell2.interfaces.websocket;

import ca.redleafsolutions.ishell2.ui.notifications.WebSocketMessage;

public class JettyWebSocketMessage implements WebSocketMessage {
	private String message;

	public JettyWebSocketMessage (String message) {
		this.message = message;
	}

	@Override
	public String toString () {
		return message;
	}
}
