package org.asf.connective.usermanager.configs;

import org.asf.cyan.api.config.Configuration;
import org.asf.rats.HttpRequest;

public class UserCreationCCFG extends Configuration<UserCreationCCFG> {

	public UserCreationCCFG(HttpRequest request) {
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

	public String group = null;
	public String username = null;
	public String password = null;
	public String productKey = null;
	public String ownerEmail = null;

}
