package ca.redleafsolutions.ishell2.interfaces.websocket;

import java.io.IOException;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import ca.redleafsolutions.SingletonException;
import ca.redleafsolutions.base.events.EventDispatcher;
import ca.redleafsolutions.ishell2.annotations.IShellInvisible;
import ca.redleafsolutions.ishell2.interfaces.http.JettyServerHelpers;
import ca.redleafsolutions.ishell2.logs.iLogger;
import ca.redleafsolutions.ishell2.ui.notifications.ChannelEvent;
import ca.redleafsolutions.json.JSONItem;
import ca.redleafsolutions.json.JSONValidationException;

public class JettyWebSocketServer extends EventDispatcher<ChannelEvent> {

	private Server server;

	//	@SuppressWarnings("serial")
	//	public static class JettySocketServlet extends WebSocketServlet { 
	//        @Override
	//        public void configure(WebSocketServletFactory factory) {
	//        	try {
	//        		factory.register(JettyWebSocket.class);
	//        	} catch (Throwable e){
	//        		iLogger.severe(e);
	//        	}
	//            
	//        }
	//    }

	static private JettyWebSocketServer instance;

	@IShellInvisible
	static public JettyWebSocketServer getInstance () {
		return instance;
	}

	public JettyWebSocketServer (JSONItem json) throws JSONValidationException, SingletonException {
		if (instance != null) {
			throw new SingletonException (this);
		}
		instance = this;
		server = new Server (json.getInt ("port"));
		try {
			if (json.has ("ssl")) {
				JSONItem ssl = json.getJSON ("ssl");
				SslContextFactory sslContextFactory = JettyServerHelpers.createContextFactory (ssl);
				SslConnectionFactory sslConnectionFactory = new SslConnectionFactory (sslContextFactory, HttpVersion.HTTP_1_1.asString ());
				HttpConnectionFactory httpConnectionFactory = new HttpConnectionFactory (new HttpConfiguration ());
				ServerConnector sslConnector = new ServerConnector (server, sslConnectionFactory, httpConnectionFactory);
				sslConnector.setPort (ssl.getInt ("port"));
				server.addConnector (sslConnector);
			}
			ServletContextHandler ctx = new ServletContextHandler ();
			ctx.setContextPath ("/");
			ctx.addServlet (JettySocketServlet.class, "/");
			server.setHandler (ctx);
			server.start ();
		} catch (Exception e) {
			iLogger.severe (e);
		}

	}

	public int getPort () {
		Connector[] conn = server.getConnectors ();
		if (conn != null && conn.length > 0) {
			return ((ServerConnector)conn[0]).getPort ();
		}
		return -1;
	}

	public int getSecuredPort () {
		Connector[] conn = server.getConnectors ();
		if (conn != null && conn.length > 1) {
			return ((ServerConnector)conn[1]).getPort ();
		}

		return -1;
	}

	public void send (String message) throws IOException {
		JettyWebSocket.getInstance ().send (message);
	}
//
//	public WebSocketHandler addWebSocket (final Class<?> webSocket, String pathSpec) {
//		WebSocketHandler wsHandler = new WebSocketHandler () {
//
//			@Override
//			public void configure (WebSocketServletFactory webSocketServletFactory) {
//				webSocketServletFactory.register (webSocket);
//			}
//		};
//		ContextHandler wsContextHandler = new ContextHandler ();
//		wsContextHandler.setHandler (wsHandler);
//		wsContextHandler.setContextPath (pathSpec); // this context path doesn't work ftm
//		return wsHandler;
//	}
//
}
