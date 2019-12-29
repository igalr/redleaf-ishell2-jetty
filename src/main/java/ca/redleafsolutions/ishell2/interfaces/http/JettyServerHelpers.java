package ca.redleafsolutions.ishell2.interfaces.http;

import java.io.File;
import java.io.FileNotFoundException;

import org.eclipse.jetty.util.ssl.SslContextFactory;

import ca.redleafsolutions.json.JSONItem;
import ca.redleafsolutions.json.JSONValidationException;

public class JettyServerHelpers {
	public static SslContextFactory createContextFactory (JSONItem ssl) throws JSONValidationException, FileNotFoundException {
		String jettyDistKeystore = ssl.getString ("keystore");
		File keystoreFile = new File (jettyDistKeystore);
		if (!keystoreFile.exists ()) {
			throw new FileNotFoundException ("Keystore file " + keystoreFile + " was not found");
		}

		SslContextFactory sslContextFactory = new SslContextFactory ();
		sslContextFactory.setKeyStorePath (keystoreFile.getAbsolutePath ());
		String kmpassword, kspassword = ssl.getString ("keystore-password");
		sslContextFactory.setKeyStorePassword (kspassword);
		try {
			kmpassword = ssl.getString ("keymanager-password");
		} catch (JSONValidationException.MissingKey e) {
			kmpassword = kspassword;
		}
		sslContextFactory.setKeyManagerPassword (kmpassword);
		sslContextFactory.setProtocol ("TLS");
		sslContextFactory.setIncludeProtocols ("TLSv1", "TLSv1.1");
		sslContextFactory.setTrustAll (true);
		sslContextFactory.setTrustStorePassword (kspassword);
		return sslContextFactory;
	}
}
