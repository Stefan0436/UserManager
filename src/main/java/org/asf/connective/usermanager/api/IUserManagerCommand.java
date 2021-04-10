package org.asf.connective.usermanager.api;

import java.io.IOException;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

/**
 * 
 * User manager commands
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IUserManagerCommand {
	
	/**
	 * Command path (such as /create or /authenticate, recommended to use configs)
	 */
	public String path();

	/**
	 * The HTTP method this command accepts (* for all)
	 */
	public String method();

	/**
	 * Runs the command
	 * 
	 * @param request  HTTP request
	 * @param response HTTP response
	 * @param server   Server used to process the request
	 * @throws IOException If processing fails
	 */
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server) throws IOException;
}
