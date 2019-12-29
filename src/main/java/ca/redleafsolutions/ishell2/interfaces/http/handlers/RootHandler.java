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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Enumeration;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import ca.redleafsolutions.ishell2.IShellInputStream;
import ca.redleafsolutions.ishell2.IShellObject;
import ca.redleafsolutions.ishell2.IShellObject.ExecutedObject;
import ca.redleafsolutions.ishell2.IShellRedirectable;
import ca.redleafsolutions.ishell2.IShellRequestSingle;
import ca.redleafsolutions.ishell2.IShellResponse;
import ca.redleafsolutions.ishell2.ParseRequestResults;
import ca.redleafsolutions.ishell2.interfaces.http.EmbeddedJettyServer;
import ca.redleafsolutions.ishell2.interfaces.http.IShellDownloadable;
import ca.redleafsolutions.ishell2.interfaces.http.MimeHandler;
import ca.redleafsolutions.ishell2.logs.iLogger;

@MultipartConfig (location = "d:/temp", fileSizeThreshold = 1024 * 1024, maxFileSize = 1024 * 1024
		* 5, maxRequestSize = 1024 * 1024 * 5 * 5)
public class RootHandler extends AbstractHandler {
	private EmbeddedJettyServer iface;

	public RootHandler (EmbeddedJettyServer iface) {
		super ();
		this.iface = iface;
	}

	@Override
	public void handle (String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
			throws IOException, ServletException {
		OutputStream os = response.getOutputStream ();
		ParseRequestResults parsed = null;
		try {
			// if call to a virtual host then transfer the handling of
			// request
			String qstr = request.getQueryString ();
			if (qstr == null)
				qstr = "";
			URI uri = URI.create (request.getRequestURI () + (qstr.length () > 0 ? "?" + qstr : ""));
			parsed = new ParseRequestResults (uri);

			MimeHandler mime = new MimeHandler ();
			Enumeration<String> hnames = request.getHeaderNames ();
			while (hnames.hasMoreElements ()) {
				String key = hnames.nextElement ();
				String value = request.getHeader (key);
				mime.addHeaderEntry (key, value);
			}

			if (request.getMethod ().equalsIgnoreCase ("post") || mime.isMultiPart ()) {
				mime.parseRequest (request.getInputStream ());
				parsed.addParams (mime.getParams ());
			} else if (!parsed.hasFormatExtension ()) {
				String path = uri.getPath ();
				int li = path.lastIndexOf ("/");
				if (li != path.length () - 1) {
					String uris = uri.getPath () + "/";
					if (uri.getQuery () != null)
						uris += "?" + uri.getQuery ();
					URI uri2 = URI.create (uris);

					response.sendRedirect (uri2.toString ());
					return;
				}
			}

			String format = parsed.getOutputFormat (iface.defaultoutput ());
			String txmime = new MimeTypes ().getMimeByExtension ("." + format);
			if (txmime != null) {
				response.addHeader ("Content-Type", txmime);
			}

			for (String key: iface.getHeaders ().keySet ()) {
				response.addHeader (key, iface.getHeaders ().get (key));
			}

			IShellRequestSingle ishellrequest = new IShellRequestSingle (parsed);
			ishellrequest.setRemote (request.getRemoteAddr () + " /1 " + request.getRemoteHost () + " /2 " + request.getRemotePort () + " /3 " + request.getRemoteUser ());
			int reqid = iLogger.logIShellRequest (ishellrequest,
					baseRequest.getRemoteInetSocketAddress ().getAddress ());

			IShellResponse ishellresponse = iface.executeAndRespond (ishellrequest, parsed);
			if (ishellresponse.getResponse () instanceof IShellObject.ExceptionObject) {
				response.setStatus (404);
			} else {
				response.setStatus (200);
			}

			boolean responseSent = false;
			if (ishellresponse.getResponse () instanceof IShellObject.ExecutedObject) {
				ExecutedObject respobj = (IShellObject.ExecutedObject)(ishellresponse.getResponse ());
				if (respobj.getObject () instanceof IShellInputStream) {
					IShellInputStream is = (IShellInputStream)respobj.getObject ();
					if (is.getMimeType () != null) {
						response.addHeader ("Content-Type", is.getMimeType ());
					}
					response.setContentLength ((int)is.length ());
					is.tunnelTo (os);
					is.close ();
					responseSent = true;
					iLogger.logIShellResponse (reqid, ": stream tunneled through. Length " + is.length ());
				} else if (respobj.getObject () instanceof IShellDownloadable) {
					IShellDownloadable ds = (IShellDownloadable)respobj.getObject ();	
					response.addHeader ("Content-Type", ds.getMimeType ());
					response.addHeader ("Content-Disposition", "attachment;filename=" + ds.getFilename ());
					response.setContentLength ((int)ds.length ());
					os.write (ds.getBuffer ());
					responseSent = true;
					iLogger.logIShellResponse (reqid, ": Downloading " + ds.length () + " bytes as " + ds.getFilename ());
					ds.close ();
				} else if (respobj.getObject () instanceof IShellRedirectable) {
					IShellRedirectable redirect = (IShellRedirectable)respobj.getObject ();
					response.addHeader ("Location", redirect.getUrl ().toString ());
					response.setStatus (307);
				}
			} else if (ishellresponse.getResponse () instanceof IShellObject.RawObject) {
				IShellObject.RawObject respobj = (IShellObject.RawObject)(ishellresponse.getResponse ());
				if (respobj.getObject () instanceof IShellRedirectable) {
					IShellRedirectable redirect = (IShellRedirectable)respobj.getObject ();
					String url = redirect.getUrl ();
					if (url != null) {
						response.addHeader ("Location", url);
						response.setStatus (307);
					}
				}
			}

			if (!responseSent) {
				String s = ishellresponse.toString ();
				byte[] bytes = s.getBytes ();
				response.setContentLength (bytes.length);
				os.write (bytes);
				os.flush ();
				iLogger.logIShellResponse (reqid, ": " + s);
			}
		} catch (Throwable e) {
			iLogger.severe (e);
		} finally {
			// attempt to delete any uploaded files (temporary)
			if (parsed != null) {
				for (Object o: parsed.getParams ().values ()) {
					if (o instanceof File) {
						((File)o).delete ();
					}
				}
			}
			try {
				os.close ();
			} catch (Exception e) {
			}
			baseRequest.setHandled (true);
		}
	}
}