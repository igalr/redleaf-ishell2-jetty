package ca.redleafsolutions.ishell2.interfaces.websocket;

import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import ca.redleafsolutions.ishell2.logs.iLogger;

@SuppressWarnings("serial")
public class JettySocketServlet extends WebSocketServlet { 
    @Override
    public void configure(WebSocketServletFactory factory) {
    	try {
    		factory.register(JettyWebSocket.class);
    	} catch (Throwable e){
    		iLogger.severe(e);
    	}
        
    }
}