package org.asf.connective.usermanager.implementation.commands;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.stream.Stream;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.IUserManagerCommand;
import org.asf.connective.usermanager.configs.UserCreationCCFG;
import org.asf.connective.usermanager.configs.physical.ActivationKeyConfig;
import org.asf.connective.usermanager.configs.physical.ProductKeyConfig;
import org.asf.rats.ConnectiveHTTPServer;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;

public class CreateCommand implements IUserManagerCommand {

	@Override
	public String path() {
		return UserManagerModule.getCreateUserCommand();
	}

	@Override
	public void run(HttpRequest request, HttpResponse response, ConnectiveHTTPServer server, Socket client) throws IOException {
		UserCreationCCFG ccfg = new UserCreationCCFG(request);
		if (ccfg.username == null || ccfg.password == null || ccfg.group == null || ccfg.ownerEmail == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Invalid content received, please format it in CCFG.\n");
			return;
		}

		if (!UserManagerModule.getAuthBackend().validateUsername(ccfg.username)) {
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
		if (!ccfg.ownerEmail
				.matches("^[A-Za-z0-9\\-_.]+\\@[A-Za-z0-9_\\-]+(\\.[A-Za-z0-9]+)?(\\.[A-Za-z0-9]+)?(\\.[A-Za-z0-9]+)?$")) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Malformed email address.\n");
			return;
		}

		if (!Stream.of(UserManagerModule.getAllowedGroups()).anyMatch(t -> t.equals(ccfg.group))) {
			response.status = 403;
			response.message = "Access denied";
			response.setContent("text/plain", "Group access denied.\n");
			return;
		}

		if (UserManagerModule.isProductGroup(ccfg.group) && ccfg.productKey == null) {
			response.status = 400;
			response.message = "Bad request";
			response.setContent("text/plain", "Missing required product key.\n");
			return;
		} else if (UserManagerModule.isProductGroup(ccfg.group)) {
			ccfg.productKey = ccfg.productKey.toUpperCase();
			boolean found = false;

			for (String key : UserManagerModule.getValidProductKeys(ccfg.group)) {
				ProductKeyConfig conf = UserManagerModule.getProductKey(ccfg.group, key);
				if (key.equals(ccfg.productKey) && conf.isStillValid()) {
					found = true;
				}
			}

			if (!found) {
				response.status = 403;
				response.message = "Access denied";
				response.setContent("text/plain", "Product key not valid.\n");
				return;
			}
		}

		if (Stream.of(UserManagerModule.getActivationKeys())
				.anyMatch(t -> t.group.equals(ccfg.group) && t.username.equals(ccfg.username))
				|| UserManagerModule.getAuthBackend().userExists(ccfg.group, ccfg.username)) {
			response.status = 403;
			response.message = "Access denied";
			response.setContent("text/plain", "Username is in use.\n");
			return;
		}

		ActivationKeyConfig key = UserManagerModule.generateUnusedActivationKey(ccfg.group, ccfg.username,
				ccfg.password.toCharArray(), ccfg.productKey);
		ccfg.password = null;

		if (UserManagerModule.isProductGroup(ccfg.group)) {
			File keyFile = UserManagerModule.getProductKeyFile(ccfg.group, ccfg.productKey);
			keyFile.delete();
		}

		HashMap<String, String> vars = new HashMap<String, String>();
		vars.put("sender", UserManagerModule.getMailSender());
		vars.put("date", new SimpleDateFormat("dd-MM-YYYY HH:mm (zz)").format(new Date()));
		vars.put("group", ccfg.group);
		vars.put("username", ccfg.username);
		vars.put("activationkey", key.key);
		vars.put("cancelkey", key.cancelKey);
		vars.put("recipient", ccfg.ownerEmail);
		try {
			vars.put("expiry\\-date",
					new SimpleDateFormat("dd-MM-YYYY").format(UserManagerModule.parseDateString(key.expiryDate)));
		} catch (ParseException e1) {
			throw new IOException(e1);
		}

		String mail = UserManagerModule.generate(UserManagerModule.getMailTemplate().trim(), vars) + "\n\n";
		String[] cmd = UserManagerModule.generate(UserManagerModule.getMailCommand(), vars);

		ProcessBuilder builder = new ProcessBuilder(cmd);
		Process mailProc = builder.start();
		mailProc.getOutputStream().write(mail.getBytes());
		mailProc.getOutputStream().close();
		try {
			mailProc.waitFor();
		} catch (InterruptedException e) {
		}
		if (mailProc.exitValue() != 0) {
			String error = new String(mailProc.getErrorStream().readAllBytes());
			response.status = 503;
			response.message = "Internal server error";
			response.setContent("text/plain", "Mail sending failed:\n" + error + "\n");

			UserManagerModule.removeActivationKey(key, false);

			return;
		}

		response.setContent("text/plain",
				"Account creation queued, activation mail has been sent.\nCancel key: " + key.cancelKey + "\n");
	}

	@Override
	public String method() {
		return "POST";
	}

}
