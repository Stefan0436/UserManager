package org.asf.connective.usermanager.util;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.connective.usermanager.api.IAuthenticationBackend;
import org.asf.rats.Memory;

/**
 * 
 * Simple class to access UserManager frontends and backends.
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class Authentication {

	/**
	 * Retrieves the current authentication backend
	 * 
	 * @return IAuthenticationBackend instance or null if a configuration mistake
	 *         was made
	 */
	public static IAuthenticationBackend getAuthBackend() {
		return UserManagerModule.getAuthBackend();
	}

	/**
	 * Retrieves the current authentication frontend
	 * 
	 * @return IAuthFrontend instance
	 */
	public static IAuthFrontend getAuthFrontend() {
		return Memory.getInstance().get("usermanager.auth.frontend").getValue(IAuthFrontend.class);
	}

}
