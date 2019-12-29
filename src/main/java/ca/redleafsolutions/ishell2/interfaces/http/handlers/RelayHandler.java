/*
 * iShell 2.0
 *
 * Copyright (c) 2010, Redleaf Solutions Ltd. All rights reserved.
 *
 * This library is proprietary software; you can not redistribute
 * without an explicit consent from Releaf Solutions Ltd.
 * The consent will detail the distribution and sale rights.
 */
package ca.redleafsolutions.ishell2.interfaces.http.handlers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;

import ca.redleafsolutions.ishell2.IShellRequest;
import ca.redleafsolutions.ishell2.ParseRequestResults;
import ca.redleafsolutions.ishell2.logs.iLogger;

public class RelayHandler extends BaseHandler {
	private URI uri;

	public RelayHandler (String key, URI uri) {
		super (key);
		this.uri = uri;
	}

	@Override
	public void handle (String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		try {
			// parse the URL
			URI _uri = URI.create (request.getRequestURI ());
			ParseRequestResults parsed = new ParseRequestResults (_uri, key);

			String u = uri.toString ();
			if (parsed.getParams ().size () > 0) {
				u += "?";
			}
			for (Entry<String, Object> entry: parsed.getParams ().entrySet ()) {
				u += entry.getKey () + "=" + entry.getValue ();
			}

			try {
				// construct a URL call to the relayed server
				URL url = new URL (u);
				HttpURLConnection conn = (HttpURLConnection)url.openConnection ();
				conn.setRequestMethod ("GET");
				// copy all the header fields to the relayed request
				for (Enumeration<String> hnames = request.getHeaderNames (); hnames.hasMoreElements ();) {
					String key = hnames.nextElement ();
					if (key != null) {
						String value = request.getHeader (key);
						if ("host".equalsIgnoreCase (key)) {
							value = uri.getHost ();
						}
						conn.setRequestProperty (key, value);
					}
				}

				// copy the header fields from the relayed response
				for (Entry<String, List<String>> entry: conn.getHeaderFields ().entrySet ()) {
					String key = entry.getKey ();
					if (key != null) {
						for (String value: entry.getValue ()) {
							response.addHeader (key, value);
						}
					}
				}

				response.setStatus (conn.getResponseCode ());
				int l = conn.getContentLength ();
				if (l > 0)
					response.setContentLength (l);

				OutputStream os = response.getOutputStream ();
				byte[] buff = new byte[4096];
				int len, total = 0;
				while ((len = conn.getInputStream ().read (buff, 0, buff.length)) > 0) {
					os.write (buff, 0, len);
					total += len;
				}
				conn.disconnect ();
				if (l <= 0) {
					response.setContentLength (total);
				}
			} catch (Exception e) {
				iLogger.severe (e);
				e.printStackTrace ();
			}
		} catch (Throwable e) {
			iLogger.severe (e);
			e.printStackTrace ();
		} finally {
			baseRequest.setHandled(true);
		}
	}

	@Override
	protected byte[] _handle (IShellRequest request) {
		throw new RuntimeException ("Code should of never reach this point");
	}
}