package ca.redleafsolutions.ishell2.interfaces.websocket;

import ca.redleafsolutions.SingletonException;
import ca.redleafsolutions.ishell2.ui.notifications.NoticiationChannelBase;
import ca.redleafsolutions.json.JSONItem;
import ca.redleafsolutions.json.JSONValidationException;

public class JettyNotificationChannel extends NoticiationChannelBase {
	private JettyWebSocketServer websockserver;

	public JettyNotificationChannel (JSONItem json) throws JSONValidationException, SingletonException {
		super ();
		websockserver = new JettyWebSocketServer (json);
		websockserver.addEventHandler (this);
	}

	@Override
	public int port () {
		return websockserver.getPort ();
	}

	@Override
	public int secureport () {
		return websockserver.getSecuredPort ();
	}

	@Override
	public boolean isRunning () {
		return websockserver != null;
	}

	@Override
	public String type () {
		return "JettyWebSocket";
	}
}
