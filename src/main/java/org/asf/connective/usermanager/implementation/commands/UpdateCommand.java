package org.asf.connective.usermanager.implementation.commands;

import java.io.IOException;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.UpdateUserCCFG;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;

public class UpdateCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getUpdateCommand();
	}

	@Override
	public String method() {
		return "POST";
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server, Socket client) throws IOException {
		UpdateUserCCFG ccfg = new UpdateUserCCFG(request);
		if (ccfg.group == null || ccfg.password == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}

		if (ccfg.username != null && !UserManagerModule.getAuthBackend().validateUsername(ccfg.username)) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Malformed username.\n");
			return;
		}
		if (!UserManagerModule.getAuthBackend().validateGroupname(ccfg.group)) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Malformed group name.\n");
			return;
		}

		AuthResult result = UserManagerModule.getAuthBackend().authenticate(ccfg.group, request, response);
		if (!result.success()) {
			response.setContent("text/plain", response.message + ".\n");
			return;
		}

		if (ccfg.username != null) {
			if (Stream.of(UserManagerModule.getActivationKeys())
					.anyMatch(t -> t.group.equals(ccfg.group) && t.username.equals(ccfg.username))
					|| UserManagerModule.getAuthBackend().userExists(ccfg.group, ccfg.username)) {
				response.status = 403;
				response.message = "Access denied";
				response.setContent("text/plain", "Username is in use.\n");
				return;
			}

			UserManagerModule.getAuthBackend().deleteUser(result.getGroup(), result.getUsername());
			for (BiConsumer<String[], String> consumer : Memory.getInstance().getOrCreate("users.change.username")
					.getValues(BiConsumer.class)) {
				consumer.accept(new String[] { result.getGroup(), result.getUsername() }, ccfg.username);
			}
			
			result.setNewUsername(ccfg.username);
			UserManagerModule.getAuthBackend().updateUser(result.getGroup(), ccfg.username,
					ccfg.password.toCharArray());
		} else {
			UserManagerModule.getAuthBackend().updateUser(result.getGroup(), result.getUsername(),
					ccfg.password.toCharArray());
		}

		result.setNewPassword(ccfg.password.toCharArray());
		ccfg.password = null;

		response.setContent("text/plain", "User has been updated.\n");
	}
}
