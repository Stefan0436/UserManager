package org.asf.connective.usermanager.implementation.commands;

import java.io.IOException;
import java.net.Socket;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.DeleteUserCCFG;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;

public class DeleteCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getDeleteCommand();
	}

	@Override
	public String method() {
		return "POST";
	}

	@Override
	@SuppressWarnings("unchecked")
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server, Socket client) throws IOException {
		DeleteUserCCFG ccfg = new DeleteUserCCFG(request);
		if (ccfg.group == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
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

		for (BiConsumer<String, String> consumer : Memory.getInstance().getOrCreate("users.delete")
				.getValues(BiConsumer.class)) {
			consumer.accept(result.getGroup(), result.getUsername());
		}

		for (Consumer<AuthResult> consumer : Memory.getInstance().getOrCreate("users.delete")
				.getValues(Consumer.class)) {
			consumer.accept(result);
		}

		UserManagerModule.getAuthBackend().deleteUser(result.getGroup(), result.getUsername());
		UserManagerModule.deleteUser(result.getGroup(), result.getUsername());
		result.deleteUser();

		response.setContent("text/plain", "User has been deleted.\n");
	}
}
