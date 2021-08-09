package org.asf.connective.usermanager.api;

import java.io.IOException;
import java.net.Socket;

import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

/**
 * 
 * Authentication frontend, the usermanager.auth.frontend key should be used to
 * select the implementation.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IAuthFrontend {

	/**
	 * Checks if the authentication header (or cookie or such) is present and valid.
	 * 
	 * @param group    User group
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return True if valid, false otherwise
	 * @throws IOException If authenticating fails.
	 */
	public boolean check(String group, HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * Checks if the authentication header (or cookie or such) is present and valid.
	 * 
	 * @param group    User group
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @param client   Client making the requst
	 * @return True if valid, false otherwise
	 * @throws IOException If authenticating fails.
	 */
	public default boolean check(String group, HttpRequest request, HttpResponse response, Socket client)
			throws IOException {
		return check(group, request, response);
	}

	/**
	 * Authenticates the HTTP request, the default uses authentication headers, but
	 * you can use login pages too.
	 * 
	 * @param group    User group
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @return Authentication result
	 * @throws IOException If authenticating fails.
	 */
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response) throws IOException;

	/**
	 * Authenticates the HTTP request, the default uses authentication headers, but
	 * you can use login pages too.
	 * 
	 * @param group    User group
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @param client   Client making the requst
	 * @return Authentication result
	 * @throws IOException If authenticating fails.
	 */
	public default AuthResult authenticate(String group, HttpRequest request, HttpResponse response, Socket client)
			throws IOException {
		return authenticate(group, request, response);
	}

}
