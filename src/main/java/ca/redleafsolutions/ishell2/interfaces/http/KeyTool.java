package ca.redleafsolutions.ishell2.interfaces.http;

import java.io.File;
import java.io.IOException;

import org.eclipse.jetty.util.security.Credential;
import org.eclipse.jetty.util.security.Password;

import ca.redleafsolutions.CommandLineRunner;

public class KeyTool {
	public String password (String username, String password) {
		return Credential.Crypt.crypt (username, new Password (password).toString ());
	}

	public String obfuscate (String password) {
		return Password.obfuscate (new Password (password).toString ());
	}

	public String md5 (String password) {
		return Credential.MD5.digest (password);
	}

	public Process exec (String cmd) throws Exception {
		CommandLineRunner cmdln = CommandLineRunner.create (cmd);
		return cmdln.exec ();
	}

	public Object create () throws IOException {
		File file = new File ("keystore");
		
		try {
			CommandLineRunner cmdln = CommandLineRunner.create ("keytool -keystore \"" + file.getAbsolutePath () + "\" -alias jetty -genkey -keyalg RSA");
			return cmdln.exec ();
		} catch (Exception e) {
			throw new IOException (e.getMessage ());
		}
//		String command = "cmd /c start keytool -keystore \"" + file.getAbsolutePath () + "\" -alias jetty -genkey -keyalg RSA";
//		Process process = Runtime.getRuntime ().exec (command);
//		return process;
	}
}
