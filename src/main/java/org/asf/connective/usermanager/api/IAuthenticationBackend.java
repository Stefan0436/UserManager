package org.asf.connective.usermanager.api;

import java.io.IOException;

import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

/**
 * 
 * Authentication backend, main authentication provider, default uses credtool.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IAuthenticationBackend {
	/**
	 * Backend name (for config file)
	 */
	public String name();

	/**
	 * Checks whether or not all required programs and libraries are present to run
	 * this backend.
	 * 
	 * @return True if all files that are needed to run are present, false
	 *         otherwise.
	 */
	public boolean available();

	/**
	 * Checks if a user exists
	 * 
	 * @param group    Group name
	 * @param username User name
	 * @return True if the user exists, false otherwise
	 * @throws IOException If reading fails
	 */
	public boolean userExists(String group, String username) throws IOException;

	/**
	 * Update/create a user
	 * 
	 * @param group    User group
	 * @param username User name
	 * @param password User password
	 * @throws IOException If creating the user fails
	 */
	public void updateUser(String group, String username, char[] password) throws IOException;

	/**
	 * Validates the given username (checks if name doesn't contain illegal
	 * characters)
	 * 
	 * @param username Username to check
	 * @return True if valid, false if it contains illegal characters
	 */
	public boolean validateUsername(String username);

	/**
	 * Validates the given group name (checks if name doesn't contain illegal
	 * characters)
	 * 
	 * @param group Group name to check
	 * @return True if valid, false if it contains illegal characters
	 */
	public boolean validateGroupname(String group);

	/**
	 * Authenticates the HTTP request
	 * 
	 * @param group    Group used
	 * @param request  HTTP request, check headers with this
	 * @param response HTTP response, should have 401 with the WWW-Authenticate
	 *                 header if the client has no authentication header, should
	 *                 also have 401 if the credentials are invalid so that the
	 *                 client knows it needs to authenticate.
	 * @return The authentication result
	 */
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response);

	/**
	 * Deletes a given user (can also be used to change usernames, remember to NOT
	 * delete the storage if doing that)
	 * 
	 * @param group    User group
	 * @param username Username
	 * @throws IOException If deleting the user fails
	 */
	public void deleteUser(String group, String username) throws IOException;
}
