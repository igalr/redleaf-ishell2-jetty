package ca.redleafsolutions.ishell2.interfaces.websocket;

import java.io.IOException;
import java.net.InetAddress;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;

import ca.redleafsolutions.StringMap;
import ca.redleafsolutions.base.events.EventDispatcher;
import ca.redleafsolutions.ishell2.logs.iLogger;
import ca.redleafsolutions.ishell2.ui.notifications.ChannelEvent;
import ca.redleafsolutions.ishell2.ui.notifications.WebSocketConnection;
import ca.redleafsolutions.ishell2.ui.notifications.WebSocketMessage;

@WebSocket
public class JettyWebSocket extends EventDispatcher<ChannelEvent> implements WebSocketConnection {
	private static final JettyWebSocket _instance = new JettyWebSocket ();

	public static JettyWebSocket getInstance () {
		return _instance;
	}

	private Session session;

	// called when the socket connection with the browser is established
	@OnWebSocketConnect
	public void handleConnect (Session session) {
		this.session = session;

		ServletUpgradeRequest r = (ServletUpgradeRequest)session.getUpgradeRequest ();
		String query = r.getQueryString ();
		StringMap map = new StringMap ();
		map.fromSearchString (query);

		JettyWebSocketServer.getInstance ().dispatchEvent (new ChannelEvent.WebSocketConnected (this, map));
	}

	// called when the connection closed
	@OnWebSocketClose
	public void handleClose (int statusCode, String reason) {
		JettyWebSocketServer.getInstance ().dispatchEvent (new ChannelEvent.WebSocketDisconnected (this));
	}

	// called when a message received from the browser
	@OnWebSocketMessage
	public void handleMessage (String message) {
		handleMessage (new JettyWebSocketMessage (message));
	}
	@Override
	public void handleMessage (WebSocketMessage message) {
		JettyWebSocketServer.getInstance ().dispatchEvent (new ChannelEvent.MessageReceived (this, message));
	}

	// called in case of an error
	@OnWebSocketError
	@Override
	public void handleError (Throwable e) {
		iLogger.warning ("Notification channel " + session + " error " + e);
		close (0);
	}

	// sends message to browser
	public void send (String message) throws IOException {
		if (session.isOpen ()) {
			session.getRemote ().sendString (message);
		}
	}

	public void close (int code) {
		session.close (code, "" + code);
	}

	@Override
	public InetAddress remote () {
		return session.getRemoteAddress ().getAddress ();
	}

	@Override
	public String toString () {
		return remote ().getHostAddress () + "/" + this.hashCode ();
	}

	@Override
	public boolean isOpen () {
		return session.isOpen ();
	}
}