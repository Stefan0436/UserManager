package org.asf.connective.usermanager.implementation;

import java.io.IOException;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class DefaultAuthFrontend implements IAuthFrontend {

	@Override
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response) throws IOException {
		return UserManagerModule.getAuthBackend().authenticate(group, request, response);
	}

	@Override
	public boolean check(String group, HttpRequest request, HttpResponse response) throws IOException {
		return UserManagerModule.getAuthBackend().authenticate(group, request, response).success();
	}

}
