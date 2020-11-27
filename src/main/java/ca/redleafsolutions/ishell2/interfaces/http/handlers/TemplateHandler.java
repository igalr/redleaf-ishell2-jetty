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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;

import ca.redleafsolutions.ObjectMap;
import ca.redleafsolutions.TemplateException;
import ca.redleafsolutions.TemplateUtils;
import ca.redleafsolutions.ishell2.IShellRequest;
import ca.redleafsolutions.ishell2.interfaces.IShellHTTPInterface;
import ca.redleafsolutions.ishell2.interfaces.http.EmbeddedJettyServer;
import ca.redleafsolutions.ishell2.interfaces.http.StreamAndLength;
import ca.redleafsolutions.ishell2.logs.iLogger;

public class TemplateHandler extends FileHandler {
	public TemplateHandler (EmbeddedJettyServer iface, String key, File file, String defaultfile) {
		super (iface, key, file, defaultfile);
	}
	
	protected StreamAndLength getStreamAndLength (File file, IShellRequest request) throws IOException, TemplateException {
		ObjectMap map = IShellHTTPInterface.templateSetup (file.toString (), request);

		Writer writer = new StringWriter ();
		InputStream is = null;
		try {
			is = new FileInputStream (file);
		} catch (FileNotFoundException e) {
			if (!file.isDirectory ()) {
				file = file.getParentFile ();
			}
			try {
				is = new FileInputStream (new File (file, defaultfile));
			} catch (FileNotFoundException e1) {
				iLogger.severe ("No default file found " + file.getAbsolutePath ());
				throw new FileNotFoundException (file.getCanonicalPath ());
			}
		} finally {
			if (is != null) {
				writer.append (TemplateUtils.evaluate (is, map));
				try {
					is.close ();
				} catch (IOException e) {
					iLogger.severe (e);
				}
			}
			if (writer != null) {
				try {
					writer.close ();
				} catch (IOException e) {
					iLogger.severe (e);
				}
			}
		}
		return new StreamAndLength (writer.toString ());
	}
}