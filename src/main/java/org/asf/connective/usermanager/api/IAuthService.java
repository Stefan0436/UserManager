package org.asf.connective.usermanager.api;

import java.util.function.Consumer;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

/**
 * 
 * Authentication services, allows for custom services, this simple interface is
 * a wrapper around a consumer passing the authentication array.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IAuthService extends Consumer<Object[]> {

	@Override
	default void accept(Object[] t) {
		run((AuthResult) t[3], (HttpRequest) t[0], (HttpResponse) t[1], (ConnectiveHTTPServer) t[2]);
	}

	/**
	 * Memory path, something like example.service.test
	 */
	public String path();

	/**
	 * Service simple name, something like example or test, this is for the url of
	 * the authenticate command
	 */
	public String name();

	/**
	 * Runs the service
	 * 
	 * @param result   Authenticated user
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @param server   Service processing the request
	 */
	public void run(AuthResult result, HttpRequest request, HttpResponse response, ConnectiveHTTPServer server);
}
