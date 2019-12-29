package ca.redleafsolutions.ishell2.interfaces.http;

import java.net.URI;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import ca.redleafsolutions.ishell2.ParseRequestResults;

public class ParsedHTTPResults extends ParseRequestResults {
	private HttpSession session;

	public ParsedHTTPResults (HttpServletRequest request, String key) {
		super (URI.create (request.getRequestURI ()), key);
//		session = request.getSession ();
	}
	
	public HttpSession getSession () {
		return session;
	}
}
