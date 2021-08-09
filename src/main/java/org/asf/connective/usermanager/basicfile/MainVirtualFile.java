package org.asf.connective.usermanager.basicfile;

import java.io.IOException;
import java.net.Socket;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.http.ProviderContext;
import org.asf.rats.http.providers.IContextProviderExtension;
import org.asf.rats.http.providers.IContextRootProviderExtension;
import org.asf.rats.http.providers.IServerProviderExtension;
import org.asf.rats.http.providers.IVirtualFileProvider;

public class MainVirtualFile extends CyanComponent implements IVirtualFileProvider, IServerProviderExtension,
		IContextRootProviderExtension, IContextProviderExtension {

	private ConnectiveHTTPServer server;
	private String contextRoot;
	private ProviderContext context;

	@Override
	public IVirtualFileProvider newInstance() {
		return new MainVirtualFile();
	}

	@Override
	public boolean match(String path, HttpRequest request) {
		if (path.equalsIgnoreCase(UserManagerModule.getBase())
				|| path.toLowerCase().startsWith(UserManagerModule.getBase() + "/")) {
			String command = path.substring(UserManagerModule.getBase().length());
			while (command.endsWith("/"))
				command = command.substring(0, command.length() - 1);

			for (IUserManagerCommand cmd : UserManagerModule.getCommands()) {
				String commandPath = cmd.path();
				while (commandPath.startsWith("/"))
					commandPath = commandPath.substring(1);
				while (commandPath.endsWith("/"))
					commandPath = commandPath.substring(0, command.length() - 1);
				commandPath = "/" + commandPath;

				if (commandPath.equalsIgnoreCase(command)
						&& (cmd.method().equals(request.method) || cmd.method().equals("*")))
					return true;
			}
		}

		return false;
	}

	@Override
	public void process(String path, String uploadMediaType, HttpRequest request, HttpResponse response, Socket client,
			String method) {

		if (UserManagerModule.getAuthBackend() == null) {
			response.status = 503;
			response.message = "Authentication Backend Unavailable";
		}

		String command = path.substring(UserManagerModule.getBase().length());
		while (command.endsWith("/"))
			command = command.substring(0, command.length() - 1);

		for (IUserManagerCommand cmd : UserManagerModule.getCommands()) {
			String commandPath = cmd.path();
			while (commandPath.startsWith("/"))
				commandPath = commandPath.substring(1);
			while (commandPath.endsWith("/"))
				commandPath = commandPath.substring(0, command.length() - 1);
			commandPath = "/" + commandPath;

			if (commandPath.equalsIgnoreCase(command)
					&& (cmd.method().equals(request.method) || cmd.method().equals("*"))) {
				try {
					if (cmd instanceof IContextRootProviderExtension) {
						cmd = cmd.newInstance();
						((IContextRootProviderExtension) cmd).provideVirtualRoot(contextRoot);
						if (cmd instanceof IContextProviderExtension) {
							((IContextProviderExtension)cmd).provide(context);
						}
					}
					cmd.run(request, response, server, client);
				} catch (IOException e) {
					error("Failed to execute usermanager command " + command, e);
					response.status = 503;
					response.message = "Internal server error";
				}
				return;
			}
		}
	}

	public boolean supportsUpload() {
		return true;
	}

	@Override
	public void provide(ConnectiveHTTPServer server) {
		this.server = server;
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
