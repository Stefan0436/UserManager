package org.asf.connective.usermanager.implementation.commands;

import java.io.IOException;
import java.net.URLEncoder;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AdminPanelFrontend;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.Memory;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IContextRootProviderExtension;

public class AdminCommands implements IUserManagerCommand, IContextRootProviderExtension, IContextProviderExtension {

	private String contextRoot = "";
	private ProviderContext context;

	@Override
	public IUserManagerCommand newInstance() {
		return new AdminCommands();
	}

	@Override
	public String path() {
		return UserManagerModule.getAdminCommand();
	}

	@Override
	public String method() {
		return "*";
	}

	@Override
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server) throws IOException {
		String url = "/" + contextRoot + "/" + UserManagerModule.getBase() + "/" + UserManagerModule.getAuthCommand();
		while (url.contains("//")) {
			url = url.replace("//", "/");
		}

		String url2 = "/" + contextRoot + "/" + UserManagerModule.getBase() + "/" + UserManagerModule.getAdminCommand();
		while (url2.contains("//")) {
			url2 = url2.replace("//", "/");
		}

		if (!Memory.getInstance().get("usermanager.auth.frontend").getValue(IAuthFrontend.class)
				.authenticate(UserManagerModule.getAdminGroup(), request, response).success()) {
			response.status = 302;
			response.message = "Authentication required";
			response.setHeader("Location", url + "?group=" + UserManagerModule.getAdminGroup() + "&target="
					+ URLEncoder.encode(url2 + (request.query.isEmpty() ? "" : "?" + request.query), "UTF-8"));
			return;
		}

		AdminPanelFrontend.runFrontend(request, response, server, context, contextRoot);
	}

	@Override
	public void provideVirtualRoot(String virtualRoot) {
		contextRoot = virtualRoot;
	}

	@Override
	public void provide(ProviderContext context) {
		this.context = context;
	}

}
