package org.asf.connective.usermanager.configs;

import org.asf.cyan.api.config.Configuration;
import org.asf.rats.HttpRequest;

public class UpdateUserCCFG extends Configuration<UpdateUserCCFG> {

	public UpdateUserCCFG(HttpRequest request) {
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
	public String password = null;
	public String username = null;

}
