/*
 * iShell 2.0
 *
 * Copyright (c) 2010, Redleaf Solutions Ltd. All rights reserved.
 *
 * This library is proprietary software; you can not redistribute
 * without an explicit consent from Releaf Solutions Ltd.
 * The consent will detail the distribution and sale rights.
 */

package ca.redleafsolutions.ishell2.interfaces.http;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import ca.redleafsolutions.SingletonException;
import ca.redleafsolutions.ishell2.iShell;
import ca.redleafsolutions.ishell2.interfaces.IShellHTTPInterface;
import ca.redleafsolutions.ishell2.interfaces.http.handlers.FileHandler;
import ca.redleafsolutions.ishell2.interfaces.http.handlers.RelayHandler;
import ca.redleafsolutions.ishell2.interfaces.http.handlers.RootHandler;
import ca.redleafsolutions.ishell2.interfaces.http.handlers.TemplateHandler;
import ca.redleafsolutions.ishell2.logs.iLogger;
import ca.redleafsolutions.json.JSONItem;
import ca.redleafsolutions.json.JSONValidationException;

public class EmbeddedJettyServer extends IShellHTTPInterface {
	private int port;
	private int secureport;
	private Server server;
	private KeyTool keytool;

	public EmbeddedJettyServer (iShell main, JSONItem params) throws JSONValidationException, IOException, SingletonException {
		super (main, params);

		String sport;
		try {
			port = params.getInt ("port");
		} catch (JSONValidationException e) {
			try {
				sport = params.getString ("port");
				port = Integer.parseInt (sport);
			} catch (JSONValidationException e1) {
				port = 80;
			}
		}

		secureport = -1;
		JSONItem ssl = null;
		if (params.has ("ssl")) {
			ssl = params.getJSON ("ssl");
			if (ssl != null) {
				try {
					secureport = ssl.getInt ("port");
				} catch (JSONValidationException e) {
					try {
						sport = ssl.getString ("port");
						secureport = Integer.parseInt (sport);
					} catch (JSONValidationException e1) {
						secureport = 443;
					}
				}
			}
		}

		String defaultfile = "index.html";
		try {
			defaultfile = params.getString ("default");
		} catch (JSONValidationException e) {
		}

		List<Handler> handlers = new ArrayList<Handler> ();

		
		addMappings (params, defaultfile, handlers);

		addVirtualHost (params, defaultfile, handlers);
        
        // adding root handler - important to be last
		ContextHandler context = createContext ("/", new RootHandler (this));
		handlers.add (context);

		
		// adding default file to each handler
		Handler[] h = new Handler[handlers.size ()];
		for (int i = 0; i < h.length; ++i) {
			h[i] = handlers.get (i);
			if (h[i] instanceof ContextHandler) {
				((ContextHandler)h[i]).setWelcomeFiles (new String[] { defaultfile });
			}
		}
		
		server = new Server ();
		
		ContextHandlerCollection contexts = new ContextHandlerCollection ();
		contexts.setHandlers (h);
		server.setHandler (contexts);
		iLogger.info ("iShell HTTP interface (Jetty) opened on port " + port + " with default output format as " + super.defaultOutput);

		boolean securedParamsFound = (secureport != -1 && ssl != null);
		
		HttpConfiguration http_config = createHttpConfig(securedParamsFound);
		ServerConnector http = createConnection(http_config, server, port, "unsecured");

		// http.setIdleTimeout(30000);
		server.addConnector(http);
		
		if (securedParamsFound) {
			SslContextFactory sslContextFactory;
			try {
				sslContextFactory = JettyServerHelpers.createContextFactory(ssl);
			} catch (FileNotFoundException e) {
				iLogger.severe (e);
				throw new JSONValidationException (e.toString ());
			}
			HttpConfiguration https_config = new HttpConfiguration (http_config);
			https_config.addCustomizer (new SecureRequestCustomizer ());

			ServerConnector https = new ServerConnector (server, new SslConnectionFactory (sslContextFactory,
					HttpVersion.HTTP_1_1.asString ()), new HttpConnectionFactory (https_config));
			https.setPort (secureport);
			http.setName("secured");
			server.addConnector(https);
			iLogger.info ("iShell HTTPS interface (Jetty) opened on port " + secureport + " with default output format as " + super.defaultOutput);
		}

		try {
			server.start ();
		} catch (InterruptedException e) {
			iLogger.severe (e);
			throw new JSONValidationException (e.toString ());
		} catch (Exception e) {
			iLogger.severe (e);
			throw new JSONValidationException (e.toString ());
		}
	}

	private ServerConnector createConnection(HttpConfiguration _config, Server _server, int _port, String name) {
		ServerConnector http = new ServerConnector (_server, new HttpConnectionFactory (_config));
		http.setPort (_port);
		http.setIdleTimeout(-1);
		http.setName(name);
		return http;
	}

	private HttpConfiguration createHttpConfig(Boolean addSecured) {
		HttpConfiguration http_config = new HttpConfiguration ();
		if (secureport != -1) {
			http_config.setSecureScheme ("https");
			http_config.setSecurePort (secureport);
		}
		
		http_config.setOutputBufferSize (32768);
		if (addSecured) {
			http_config.setSecurePort(secureport);
			http_config.setSecureScheme("https");
		}
		return http_config;
	}

	public KeyTool keytool () {
		if (keytool == null)
			keytool = new KeyTool ();
		return keytool;
	}
	
	private void addVirtualHost (JSONItem params, String defaultfile, List<Handler> handlers) {
		try {
			JSONItem vhosts = params.getJSON ("virtualhost");
			for (Object okey: vhosts.listKeys ()) {
				String key = okey.toString ();
				key = key.toLowerCase ();
				JSONItem o = vhosts.getJSON (key);
				String type = o.getString ("type");
				if (mapping != null)
					mapping.add (key);

				if ("native".equals (type)) {
					String directory = o.getString ("directory");
					handlers.add (createVirtualHost (key, new FileHandler (this, key, new File (directory), defaultfile)));
				} else if ("template".equals (type)) {
					String directory = o.getString ("directory");
					handlers.add (createVirtualHost (key, new TemplateHandler (this, key, new File (directory), defaultfile)));
				} else if ("relay".equals (type)) {
					String remote = o.getString ("remote");
					handlers.add (createVirtualHost (key, new RelayHandler (key, URI.create (remote))));
				}
			}
		} catch (Exception e) {
			// no virtual hosts key
		}
	}

	private void addMappings (JSONItem params, String defaultfile, List<Handler> handlers) {
		try {
			JSONItem routing = params.getJSON ("routing");
			for (Object okey: routing.listKeys ()) {
				String key = okey.toString ();
				key = key.toLowerCase ();
				JSONItem o = routing.getJSON (key);
				String type = o.getString ("type");
				if (mapping != null)
					mapping.add (key);
				if ("native".equals (type)) {
					String directory = o.getString ("directory");
					handlers.add (createContext ("/" + key, new FileHandler (this, key, new File (directory), defaultfile)));
				} else if ("template".equals (type)) {
					String directory = o.getString ("directory");
					handlers.add (createContext ("/" + key, new TemplateHandler (this, key, new File (directory), defaultfile)));
				} else if ("relay".equals (type)) {
					String remote = o.getString ("remote");
					handlers.add (createContext ("/" + key, new RelayHandler (key, URI.create (remote))));
				} else if ("websocket".equals (type)) {
					// ServerContainer wscontainer =
					// WebSocketServerContainerInitializer.configureContext(context);
					// TODO complete implementation of the websocket handler
				}
			}
		} catch (Exception e) {
			// no routing key
		}
	}

	private ContextHandler createContext (String path, Handler handler) {
		ContextHandler context = new ContextHandler (path);
		context.setHandler (handler);
		return context;
	}

	private ContextHandler createVirtualHost (String host, Handler handler) {
		ContextHandler context = new ContextHandler ("/");
		context.setVirtualHosts (new String[] { host });
		context.setHandler (handler);
		return context;
	}

	@Override
	public String info () {
		String s = "Jetty (ver. " + Server.getVersion () + ") HTTP server on port " + port
				+ (secureport != -1 ? ", HTTPS server on port " + secureport : "");
		for (Handler h: server.getHandlers ()) {
			if (h instanceof ContextHandlerCollection) {
				for (Handler hh: ((ContextHandlerCollection)h).getHandlers ()) {
					s += "\n" + handlerToString ((ContextHandler)hh);
				}
			} else {
				s += "\n" + handlerToString ((ContextHandler)h);
			}
		}
		return s;
	}

	private String handlerToString (ContextHandler ch) {
		String s = "";
		String[] t = ch.getVirtualHosts ();
		if ((t != null) && (t.length > 0)) {
			int counter = 0;
			for (String vhost: t) {
				if (counter++ > 0)
					s += ", ";
				s += vhost + ch.getContextPath ();
			}
		} else {
			s += "localhost" + ch.getContextPath ();
		}

		t = ch.getWelcomeFiles ();
		if ((t != null) && (t.length > 0)) {
			s += " (";
			int counter = 0;
			for (String welcomefile: t) {
				if (counter++ > 0)
					s += ", ";
				s += welcomefile;
			}
			s += ")";
		}

		s += ", : : " + ch.getHandler ();
		return s;
	}
}