package org.asf.connective.usermanager.api;

import java.util.Map;

import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;

/**
 * 
 * Admin commands - run securly behind the admin interface, these commands are
 * to manage the server.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IAdminCommand {

	/**
	 * Command id, should only be alphanumeric.
	 */
	public String id();

	/**
	 * The argument specification for this command, defines what arguments are
	 * required and which are not.
	 */
	public ArgumentSpecification[] specification();

	/**
	 * Prepares to run the command.
	 * 
	 * @param arguments   Command argument map
	 * @param request     HTTP request
	 * @param response    HTTP response
	 * @param server      Server processing the request
	 * @param context     File provider context
	 * @param contextRoot Context root
	 */
	public IAdminCommand setup(Map<String, Object> arguments, HttpRequest request, HttpResponse response,
			ConnectiveHTTPServer server, ProviderContext context, String contextRoot);

	/**
	 * Instanciates the command
	 */
	public IAdminCommand newInstance();

	/**
	 * Runs the command
	 */
	public void run();
	
	/**
	 * Retrieves the result
	 * @return True if successful, false otherwise.
	 */
	public boolean result();

	/**
	 * Retrieves the output
	 * @return Output value map
	 */
	public Map<String, String> output();

}
