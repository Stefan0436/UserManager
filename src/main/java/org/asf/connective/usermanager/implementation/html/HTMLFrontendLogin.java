package org.asf.connective.usermanager.implementation.html;

import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AuthResult;
import org.asf.connective.usermanager.api.IAuthFrontend;
import org.asf.connective.usermanager.implementation.DefaultAuthFrontend;
import org.asf.connective.usermanager.util.ParsingUtil;
import org.asf.rats.HttpRequest;
import org.asf.rats.HttpResponse;
import org.asf.rats.IAuthenticationProvider;
import org.asf.rats.Memory;

public class HTMLFrontendLogin implements IAuthFrontend {

	private static HashMap<String, AuthResult> authenticatedUsers = new HashMap<String, AuthResult>();
	static {
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(60 * 240 * 1000);
				} catch (InterruptedException e) {
					break;
				}
				authenticatedUsers.clear();
			}
		}, "Periodic authentication cleanup").start();
	}

	@Override
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response) throws IOException {
		if (request.headers.containsKey("Authorization") || request.headers.containsKey("X-Use-HTTP-Authentication")) {
			return new DefaultAuthFrontend().authenticate(group, request, response);
		}

		String file = UserManagerModule.getAuthCommand();
		while (file.startsWith("/"))
			file = file.substring(1);
		while (file.endsWith("/"))
			file = file.substring(0, file.length() - 1);
		while (file.contains("//"))
			file = file.replace("//", "/");

		String message = "";

		HashMap<String, String> query = ParsingUtil.parseQuery(request.query);
		if (query.getOrDefault("login", "invalid").equals("check") && request.method.equals("POST")) {
			HashMap<String, String> user = ParsingUtil.parseQuery(request.getRequestBody());
			if (!user.isEmpty()) {
				String username = user.get("user");
				String password = user.get("pass");

				if (Memory.getInstance().get("connective.standard.authprovider").getValue(IAuthenticationProvider.class)
						.authenticate(group, username, password.toCharArray())) {
					char[] pass = password.toCharArray();
					password = null;
					AuthResult res = new AuthResult(group, username, pass);
					for (int i = 0; i < pass.length; i++)
						pass[i] = 0;

					String session = genSessionKey();
					while (authenticatedUsers.containsKey(group + "." + session)) {
						session = genSessionKey();
					}

					if (request.query.contains("?login=check&")) {
						request.query = request.query.replace("?login=check&", "?");
					} else if (request.query.contains("?login=check")) {
						request.query = request.query.replace("?login=check", "");
					} else if (request.query.contains("&login=check")) {
						request.query = request.query.replace("&login=check", "");
					}

					response.status = 302;
					response.message = "File found";

					response.setHeader("Set-Cookie", "session=" + session + "; ");
					response.setHeader("Location", file + "?" + request.query);

					authenticatedUsers.put(group + "." + session, res);
					return res;
				} else {
					message = "Invalid credentials";
				}
			}
		}
		if (!request.query.isEmpty() && request.query.contains("?login=check&")) {
			request.query = request.query.replace("?login=check&", "?");
		} else if (!request.query.isEmpty() && request.query.contains("?login=check")) {
			request.query = request.query.replace("?login=check", "");
		} else if (!request.query.isEmpty() && request.query.contains("&login=check")) {
			request.query = request.query.replace("&login=check", "");
		}

		String[] cookieString = request.headers.getOrDefault("Cookie", "").split("; ");
		HashMap<String, String> cookies = null;

		String cookieQuery = "";
		for (String cookie : cookieString) {
			if (!cookieQuery.isEmpty())
				cookieQuery += "&";
			cookieQuery += cookie;
		}
		cookies = ParsingUtil.parseQuery(cookieQuery);

		if (!cookies.containsKey("session") || !authenticatedUsers.containsKey(group + "." + cookies.get("session"))) {
			response.setContent("text/html", "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n"
					+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n" + "<style>\r\n"
					+ "body {font-family: Arial, Helvetica, sans-serif;}\r\n" + "\r\n"
					+ "/* Full-width input fields */\r\n" + "input[type=text], input[type=password] {\r\n"
					+ "  width: 100%;\r\n" + "  padding: 12px 20px;\r\n" + "  margin: 8px 0;\r\n"
					+ "  display: inline-block;\r\n" + "  border: 1px solid #ccc;\r\n" + "  box-sizing: border-box;\r\n"
					+ "}\r\n" + "\r\n" + "/* Set a style for all buttons */\r\n" + "button {\r\n"
					+ "  background-color: #2200ff;\r\n" + "  color: white;\r\n" + "  padding: 14px 20px;\r\n"
					+ "  margin: 8px 0;\r\n" + "  border: none;\r\n" + "  cursor: pointer;\r\n" + "  width: 100%;\r\n"
					+ "}\r\n" + "\r\n" + "button:hover {\r\n" + "  opacity: 0.8;\r\n" + "}\r\n" + "\r\n"
					+ "/* Center the image and position the close button */\r\n" + ".imgcontainer {\r\n"
					+ "  text-align: center;\r\n" + "  margin: 24px 0 12px 0;\r\n" + "  position: relative;\r\n"
					+ "}\r\n" + "\r\n" + ".container {\r\n" + "  padding: 16px;\r\n" + "}\r\n" + "\r\n"
					+ "span.error {\r\n" + "  color:red;\r\n float: right;\r\n size: auto;\r\n"
					+ "  padding-top: 0px;\r\n" + "}\r\n" + "\r\n" + "/* The Modal (background) */\r\n" + ".modal {\r\n"
					+ "  display: none; /* Hidden by default */\r\n" + "  position: fixed; /* Stay in place */\r\n"
					+ "  z-index: 1; /* Sit on top */\r\n" + "  left: 0;\r\n" + "  top: 0;\r\n"
					+ "  width: 100%; /* Full width */\r\n" + "  height: 100%; /* Full height */\r\n"
					+ "  overflow: auto; /* Enable scroll if needed */\r\n"
					+ "  background-color: rgb(0,0,0); /* Fallback color */\r\n"
					+ "  background-color: rgba(0,0,0,0.4); /* Black w/ opacity */\r\n" + "  padding-top: 60px;\r\n"
					+ "}\r\n" + "\r\n" + "/* Modal Content/Box */\r\n" + ".modal-content {\r\n"
					+ "  background-color: #fefefe;\r\n"
					+ "  margin: 5% auto 15% auto; /* 5% from the top, 15% from the bottom and centered */\r\n"
					+ "  border: 1px solid #888;\r\n"
					+ "  width: 80%; /* Could be more or less, depending on screen size */\r\n" + "}\r\n" + "\r\n"
					+ "/* The Close Button (x) */\r\n" + ".close {\r\n" + "  position: absolute;\r\n"
					+ "  right: 25px;\r\n" + "  top: 0;\r\n" + "  color: #000;\r\n" + "  font-size: 35px;\r\n"
					+ "  font-weight: bold;\r\n" + "}\r\n" + "\r\n" + ".close:hover,\r\n" + ".close:focus {\r\n"
					+ "  color: red;\r\n" + "  cursor: pointer;\r\n" + "}\r\n" + "\r\n" + "/* Add Zoom Animation */\r\n"
					+ (!message.isEmpty() ? ""
							: ".animate {\r\n" + "  -webkit-animation: animatezoom 1s;\r\n"
									+ "  animation: animatezoom 1s\r\n" + "}")
					+ "\r\n" + "\r\n" + "@-webkit-keyframes animatezoom {\r\n"
					+ "  from {-webkit-transform: scale(0)} \r\n" + "  to {-webkit-transform: scale(1)}\r\n" + "}\r\n"
					+ "  \r\n" + "@keyframes animatezoom {\r\n" + "  from {transform: scale(0)} \r\n"
					+ "  to {transform: scale(1)}\r\n" + "}\r\n" + "\r\n"
					+ "/* Change styles for span and cancel button on extra small screens */\r\n"
					+ "@media screen and (max-width: 300px) {\r\n" + "  span.error {\r\n"
					+ "     color:red;\r\n    display: block;\r\n" + "     float: none;\r\n" + "  }\r\n"
					+ "  .cancelbtn {\r\n" + "     width: 100%;\r\n" + "  }\r\n" + "}\r\n" + "\r\n" + ".welcome {\r\n"
					+ "text-align: center\r\n" + "}\r\n" + "</style>\r\n" + "</head>\r\n" + "<body>\r\n" + "\r\n"
					+ "<form class=\"modal-content animate\" action=\"" + file + "?"
					+ (request.query == null || request.query.isEmpty() ? "" : request.query + "&")
					+ "login=check\" method=\"post\">\r\n"
					+ "	<h1 class=\"welcome\">Welcome, please log in to a valid account to access the requested resource.</h1>\r\n"
					+ "\r\n" + "    <div class=\"container\">\r\n"
					+ "      <label for=\"user\"><b>Username</b></label>\r\n"
					+ "      <input type=\"text\" placeholder=\"Enter Username\" name=\"user\" required>\r\n" + "\r\n"
					+ "      <label for=\"pass\"><b>Password</b></label>\r\n"
					+ "      <input type=\"password\" placeholder=\"Enter Password\" name=\"pass\" required>\r\n"
					+ "        \r\n" + "      <button type=\"submit\">Login</button>\r\n" + "    </div>\r\n" + "\r\n"
					+ "    <div class=\"container\" style=\"background-color:#f1f1f1\">\r\n"
					+ "      <span class=\"error\">" + message + "</span>\r\n"
					+ "		  <a class=\"credits\">Credits to W3Schools for the form.</a>\r\n" + "    </div>\r\n"
					+ "  </form>\r\n" + "\r\n" + "</body>\r\n" + "</html>\r\n" + "");

			return new AuthResult();
		} else {
			response.setContent("text/plain", "OK");
			return authenticatedUsers.get(group + "." + cookies.get("session"));
		}
	}

	private String genSessionKey() {
		return System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
	}

}