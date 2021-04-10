package org.asf.connective.usermanager.configs;

import org.asf.cyan.api.config.Configuration;
import org.asf.rats.HttpRequest;

public class UserActivationCCFG extends Configuration<UserActivationCCFG> {

	public UserActivationCCFG(HttpRequest request) {
		if (request.headers.containsKey("Content-Length"))
			this.readAll(request.getRequestBody());
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String activationKey = null;

}
