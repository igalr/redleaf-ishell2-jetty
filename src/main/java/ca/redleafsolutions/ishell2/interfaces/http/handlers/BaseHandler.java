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
import java.net.URI;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import ca.redleafsolutions.ishell2.IShellException;
import ca.redleafsolutions.ishell2.IShellRequest;
import ca.redleafsolutions.ishell2.IShellRequestSingle;
import ca.redleafsolutions.ishell2.ParseRequestResults;
import ca.redleafsolutions.ishell2.logs.iLogger;

abstract class BaseHandler extends AbstractHandler {
	protected String key;

	public BaseHandler (String key) {
		this.key = key;
	}

	abstract protected byte[] _handle (IShellRequest request) throws IShellException.ResourceNotFound, IOException;

	@Override
	public void handle (String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		OutputStream os = response.getOutputStream ();
		try {
			IShellRequestSingle ishellrequest = new IShellRequestSingle (new ParseRequestResults (URI.create (request.getRequestURI ()), key));
//			IShellRequest ishellrequest = new IShellRequest (new ParsedHTTPResults (request, key));
			int reqid = iLogger.logIShellRequest (ishellrequest, baseRequest.getRemoteInetSocketAddress ().getAddress ());

			try {
				// iLogger.info (t.getRemoteAddress ().getAddress () + ">> "
				// + uri.toString ());
				byte[] bytes = _handle (ishellrequest);
				// t.sendResponseHeaders (200, bytes.length);
				response.setStatus (200);
				response.setContentLength (bytes.length);
				os.write (bytes);
				iLogger.logIShellResponse (reqid, " HTTP (200) length: " + bytes.length);
			} catch (IShellException.ResourceNotFound e) {
				String s = "'" + e.getResource () + "' was not found (404)";
				// t.sendResponseHeaders (404, s.length ());
				response.setStatus (404);
				response.setContentLength (s.length ());
				os.write (s.getBytes ());
				iLogger.logIShellResponse (reqid, " HTTP (404) - " + e.toString ());
			} catch (IOException e) {
				String s = "Internal server error (500)";
				// t.sendResponseHeaders (500, s.length ());
				response.setStatus (500);
				response.setContentLength (s.length ());
				os.write (s.getBytes ());
				iLogger.logIShellResponse (reqid, " HTTP (500) - " + e.toString ());
			}
		} catch (Throwable e) {
			iLogger.severe (e.toString ());
			e.printStackTrace ();
		} finally {
			baseRequest.setHandled (true);
		}
	}
}
