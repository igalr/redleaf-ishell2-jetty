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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import ca.redleafsolutions.ishell2.IShellRequest;
import ca.redleafsolutions.ishell2.IShellRequestSingle;
import ca.redleafsolutions.ishell2.ParseRequestResults;
import ca.redleafsolutions.ishell2.interfaces.http.EmbeddedJettyServer;
import ca.redleafsolutions.ishell2.interfaces.http.StreamAndLength;
import ca.redleafsolutions.ishell2.logs.iLogger;

public class FileHandler extends AbstractHandler {
	private EmbeddedJettyServer iface;
	protected String key;
	protected File root;
	protected String defaultfile;

	public FileHandler (EmbeddedJettyServer iface, String key, File file, String defaultfile) {
		this.iface = iface;
		this.key = key;
		this.root = file;
		this.defaultfile = defaultfile;
	}

	@Override
	public void handle (String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		long tic = System.nanoTime ();
		InputStream is = null;
		OutputStream os = response.getOutputStream ();
		ParseRequestResults parsed = null;

		String query = request.getQueryString ();
		if ((query != null) && (query.length () > 0)) {
			query = "?" + query;
		} else {
			query = "";
		}
		URI uri = URI.create (request.getRequestURI () + query);
		parsed = new ParseRequestResults (uri);

		IShellRequestSingle ishellrequest = new IShellRequestSingle (new ParseRequestResults (uri, key));
		int reqid = iLogger.logIShellRequest (ishellrequest, baseRequest.getRemoteInetSocketAddress ().getAddress ());

		File file = new File (root, ishellrequest.getPathString ());

		String format = parsed.getOutputFormat (iface.defaultoutput ());
		String txmime = new MimeTypes ().getMimeByExtension ("." + format);
		if (txmime != null) {
			response.addHeader ("Content-Type", txmime);
			if (!txmime.startsWith ("text/")) {
				response.addHeader ("Accept-Ranges", "bytes");
			}
		}

		for (Entry<String, String> hdr:iface.getHeaders ().entrySet ()) {
			response.addHeader (hdr.getKey (), hdr.getValue ());
		}
		
		try {
			StreamAndLength strmlen;
			if ((txmime != null) && !txmime.startsWith ("text/")) {
				strmlen = new StreamAndLength (file, ishellrequest);
			} else {
				strmlen = getStreamAndLength (file, ishellrequest);
			}
			if (file.isDirectory ()) {
				file = new File (file, defaultfile);
			}

			response.setStatus (200);
			response.setContentLength ((int)strmlen.length ());

			is = strmlen.getInputStream ();
			int len;
			byte[] buff = new byte[1024];
			while ((len = is.read (buff)) > 0) {
				os.write (buff, 0, len);
			}
			long duration = (System.nanoTime () - tic) / 1000000;
			iLogger.logIShellResponse (reqid, " HTTP (200) length: " + file.length () + " bytes. Duration: " + duration + "ms");
		} catch (FileNotFoundException e) {
			String s = "'" + file + "' was not found (404)";
			response.setStatus (404);
			response.setContentLength (s.length ());
			os.write (s.getBytes ());
			iLogger.logIShellResponse (reqid, " HTTP (404) - file " + file.getAbsolutePath () + " was not found");
		} catch (IOException e) {
			String s = "Internal server error (500)";
			// t.sendResponseHeaders (500, s.length ());
			response.setStatus (500);
			response.setContentLength (s.length ());
			os.write (s.getBytes ());
			iLogger.logIShellResponse (reqid, " HTTP (500) - " + e.toString ());
		} catch (Throwable e) {
			iLogger.severe (e.toString ());
			e.printStackTrace ();
		} finally {
			baseRequest.setHandled (true);
		}
	}

	protected StreamAndLength getStreamAndLength (File file, IShellRequest request) throws IOException {
		return new StreamAndLength (file, request);
	}
}
