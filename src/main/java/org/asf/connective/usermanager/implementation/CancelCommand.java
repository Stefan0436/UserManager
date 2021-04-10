package org.asf.connective.usermanager.implementation;

import java.io.IOException;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.CancelActivationCCFG;
import org.asf.connective.usermanager.configs.physical.ActivationKeyConfig;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class CancelCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getCancelCommand();
	}

	@Override
	public String method() {
		return "POST";
	}

	@Override
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server) throws IOException {
		CancelActivationCCFG ccfg = new CancelActivationCCFG(request);
		if (ccfg.cancelKey == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}

		ActivationKeyConfig key = UserManagerModule.getActivationKeyByCancelKey(ccfg.cancelKey);
		if (key == null) {
			response.status = 403;
			response.message = "Access denied";
			response.setContent("text/plain", "Invalid cancel key.\n");
			return;
		}

		try {
			UserManagerModule.removeActivationKey(key, false);
			response.setContent("text/plain", "User activation has been cancelled.\n");
		} catch (IOException ex) {
			response.status = 503;
			response.message = "Internal server error";
			response.setContent("text/plain", "Cancelling user request failed, key remains, error: "
					+ ex.getClass().getTypeName() + ": " + ex.getMessage() + ".\n");
		}
	}

}
