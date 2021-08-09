package org.asf.connective.usermanager.implementation.commands;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.Base64;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.UserActivationCCFG;
import org.asf.connective.usermanager.configs.physical.ActivationKeyConfig;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class ActivateCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getActivateCommand();
	}

	@Override
	public String method() {
		return "POST";
	}

	@Override
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server, Socket client) throws IOException {
		UserActivationCCFG ccfg = new UserActivationCCFG(request);
		if (ccfg.activationKey == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}

		ActivationKeyConfig key = UserManagerModule.getActivationKey(ccfg.activationKey);
		if (key == null) {
			response.status = 403;
			response.message = "Access denied";
			response.setContent("text/plain", "Invalid activation key.\n");
			return;
		}

		try {
			UserManagerModule.getAuthBackend().updateUser(key.group, key.username,
					new String(Base64.getDecoder().decode(key.password)).toCharArray());

			if (key.productKey != null) {
				File subscribedKey = UserManagerModule.getSubscribedProductKeyFile(key.group, key.productKey.key);
				if (!subscribedKey.getParentFile().exists())
					subscribedKey.getParentFile().mkdirs();

				subscribedKey.createNewFile();
			}

			UserManagerModule.removeActivationKey(key, true);
			response.setContent("text/plain", "User has been activated, username: " + key.username + "\n");
		} catch (IOException ex) {
			response.status = 503;
			response.message = "Internal server error";
			response.setContent("text/plain", "User creation failed, key remains, error: " + ex.getClass().getTypeName()
					+ ": " + ex.getMessage() + ".\n");
		}
	}

}
