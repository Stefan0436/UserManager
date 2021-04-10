package org.asf.connective.usermanager.backends;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;

import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthenticationBackend;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.IAuthenticationProvider;
import org.asf.rats.Memory;

public class CredToolBackend implements IAuthenticationBackend {

	public String jvm = ProcessHandle.current().info().command().get();
	public String args[] = new String[] { "-cp", "%cp%", "org.asf.connective.standalone.main.CredentialTool" };

	public class ProcessResult {
		public String output = "";
		public String error = "";
		public int exitCode = 0;
	}

	public ProcessResult runCredTool(String... args) throws IOException {
		String[] cmd = new String[args.length + this.args.length + 1];
		cmd[0] = jvm;
		for (int i = 0; i < this.args.length; i++) {
			cmd[i + 1] = this.args[i].replace("%cp%", System.getProperty("java.class.path"));
		}
		for (int i = 0; i < args.length; i++) {
			cmd[i + 1 + this.args.length] = args[i];
		}

		ProcessBuilder builder = new ProcessBuilder(cmd);
		Process proc = builder.start();
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
		}

		ProcessResult res = new ProcessResult();
		res.error = new String(proc.getErrorStream().readAllBytes());
		res.output = new String(proc.getInputStream().readAllBytes());
		res.exitCode = proc.exitValue();
		return res;
	}

	public String[] getUsers(String group) throws IOException {
		ProcessResult res = runCredTool("/ls");
		String[] lines = res.output.replaceAll("\r", "").split("\n");
		lines = Arrays.copyOfRange(lines, 1, lines.length);

		ArrayList<String> users = new ArrayList<String>();

		for (String line : lines) {
			line = line.substring(3);
			String user = line.substring(0, line.lastIndexOf(" ("));
			String usergroup = line.substring(line.lastIndexOf("(") + 1, line.lastIndexOf(")"));
			if (usergroup.equals(group) || group.equals("*")) {
				users.add(user);
			}
		}

		return users.toArray(t -> new String[t]);
	}

	@Override
	public String name() {
		return "credtool";
	}

	@Override
	public boolean available() {
		try {
			Class.forName("org.asf.connective.standalone.main.CredentialTool");
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean userExists(String group, String username) throws IOException {
		if (System.getProperty("os.name").toLowerCase().contains("windows")
				|| (System.getProperty("os.name").toLowerCase().contains("win")
						&& !System.getProperty("os.name").toLowerCase().contains("darwin"))) {
			for (String user : getUsers(group)) {
				if (user.equalsIgnoreCase(username))
					return true;
			}
		} else {
			for (String user : getUsers(group)) {
				if (user.equals(username))
					return true;
			}
		}
		return false;
	}

	@Override
	public boolean validateUsername(String username) {
		return username.matches("^[A-Za-z0-9\\-@. ']+$");
	}

	@Override
	public boolean validateGroupname(String group) {
		return group.matches("^[A-Za-z0-9]+$");
	}

	@Override
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response) {
		if (!request.headers.containsKey("Authorization")) {
			response.status = 401;
			response.message = "Authorization required";
			response.setHeader("WWW-Authenticate", "Basic realm=" + group);
			return new AuthResult();
		} else {
			String header = request.headers.get("Authorization");

			String type = header.substring(0, header.indexOf(" "));
			String cred = header.substring(header.indexOf(" ") + 1);

			if (type.equals("Basic")) {
				cred = new String(Base64.getDecoder().decode(cred));
				String username = cred.substring(0, cred.indexOf(":"));
				String password = cred.substring(cred.indexOf(":") + 1);

				try {
					if (Memory.getInstance().get("connective.standard.authprovider")
							.getValue(IAuthenticationProvider.class)
							.authenticate(group, username, password.toCharArray())) {

						AuthResult res = new AuthResult(group, username, password.toCharArray());
						password = null;

						return res;
					} else {
						response.status = 401;
						response.message = "Credentials invalid";
						password = null;
						return new AuthResult();
					}
				} catch (IOException e) {
					response.status = 401;
					response.message = "Authorization required";
					password = null;
					return new AuthResult();
				}
			} else {
				response.status = 403;
				response.message = "Access denied";
				return new AuthResult();
			}
		}
	}

	@Override
	public void updateUser(String group, String username, char[] password) throws IOException {
		IOException ex = null;
		ProcessResult result = null;
		try {
			result = runCredTool(group, username, new String(password));
		} catch (IOException e) {
			ex = e;
		}

		for (int i = 0; i < password.length; i++) {
			password[i] = 0;
		}

		if (ex != null) {
			throw ex;
		} else {
			if (result.exitCode != 0) {
				throw new IOException("CredTool Error:\n" + result.error);
			}
		}
	}

	@Override
	public void deleteUser(String group, String username) throws IOException {
		ProcessResult result = runCredTool("/rmuser", group, username);
		if (result.exitCode != 0) {
			throw new IOException("CredTool Error:\n" + result.error);
		}
	}

}
