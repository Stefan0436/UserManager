package org.asf.connective.usermanager.api;

import java.io.IOException;

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

}
