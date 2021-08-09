package org.asf.connective.usermanager.implementation.commands;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.function.Consumer;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.util.ParsingUtil;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;

public class AuthenticateCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getAuthCommand();
	}

	@Override
	public String method() {
		return "*";
	}

	@SuppressWarnings("unchecked")
	@Override
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server, Socket client)
			throws IOException {
		String service = null;
		String target = null;
		String group = UserManagerModule.getAllowedGroups()[0];

		if (request.query != null && !request.query.isEmpty()) {
			HashMap<String, String> mp = ParsingUtil.parseQuery(request.query);
			service = mp.get("service");
			target = mp.get("target");
			group = mp.getOrDefault("group", group);
		}

		AuthResult result = Memory.getInstance().get("usermanager.auth.frontend").getValue(IAuthFrontend.class)
				.authenticate(group, request, response, client);
		if (result.success()) {
			response.setContent("text/html", "OK");
			if (target != null) {
				response.status = 302;
				response.message = "File found";
				response.setHeader("Location", target);
			} else if (service != null) {
				if (!UserManagerModule.isValidService(service)) {
					response.status = 404;
					response.message = "Service not found";
					response.setContent("text/html", (String) null);
					return;
				}
				service = UserManagerModule.getService(service);
				if (Memory.getInstance().get("auth.service." + service) == null) {
					response.status = 404;
					response.message = "Service not found";
					response.setContent("text/html", (String) null);
					return;
				}

				for (Consumer<Object[]> consumer : Memory.getInstance().get("auth.service." + service)
						.getValues(Consumer.class)) {
					consumer.accept(new Object[] { request, response, server, result });
				}
			}
		}
	}

}
