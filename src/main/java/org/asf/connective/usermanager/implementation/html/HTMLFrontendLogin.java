package org.asf.connective.usermanager.implementation.html;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

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

	private static HashMap<String, Session> authenticatedUsers = new HashMap<String, Session>();

	private static class Session {
		public Date expiry;
		public AuthResult user;
	}

	static {
		Memory.getInstance().getOrCreate("users.delete").<Consumer<AuthResult>>append((user) -> {
			for (String key : new ArrayList<String>(authenticatedUsers.keySet())) {
				while (true) {
					try {
						Session ses = authenticatedUsers.get(key);
						if (ses != null && ses.user.getGroup().equals(user.getGroup())
								&& ses.user.getUsername().equals(user.getUsername())) {
							authenticatedUsers.remove(key);
						}
						break;
					} catch (Exception e) {
					}
				}
			}
		});
		new Thread(() -> {
			while (true) {
				try {
					Thread.sleep(10 * 60 * 1000);
				} catch (InterruptedException e) {
					break;
				}
				for (String key : new ArrayList<String>(authenticatedUsers.keySet())) {
					while (true) {
						try {
							Session ses = authenticatedUsers.get(key);
							if (ses != null) {
								if (new Date().after(ses.expiry)) {
									authenticatedUsers.remove(key);
								}
							}
							break;
						} catch (Exception e) {

						}
					}
				}
			}
		}, "Periodic authentication cleanup").start();
	}

	@Override
	public AuthResult authenticate(String group, HttpRequest request, HttpResponse response) throws IOException {
		if (request.headers.containsKey("Authorization") || request.headers.containsKey("X-Use-HTTP-Authentication")) {
			return new DefaultAuthFrontend().authenticate(group, request, response);
		}

		String submitProps = "";
		String buttonBackground = "#2200ff";
		String lblColor = "red";

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

					buttonBackground = "#b5bdc9";
					submitProps += " disabled";
					lblColor = "#ffa200";
					message = "Loading user container... Please wait...";

					String js = "<script>\n";
					js += "document.getElementsByClassName('container')[0].style.display = 'none';\t";
					js += "$(window).bind(\"load\", function() { \r\n";
					js += "\twindow.location = window.location + \"&login=final\";\n";
					js += "});\n";
					js += "</script>";

					displayMessages(response, request, group, message, buttonBackground, lblColor, file, submitProps,
							js);

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

					if (request.query.contains("?login=final&")) {
						request.query = request.query.replace("?login=final&", "?");
					} else if (request.query.contains("?login=final")) {
						request.query = request.query.replace("?login=final", "");
					} else if (request.query.contains("&login=final")) {
						request.query = request.query.replace("&login=final", "");
					}

					response.status = 302;
					response.message = "File found";

					Calendar cal = Calendar.getInstance();
					cal.add(Calendar.HOUR_OF_DAY, 2);

					Session ses = new Session();
					ses.user = res;
					ses.expiry = cal.getTime();

					response.setHeader("Set-Cookie", group + ".session=" + session + "; Expires="
							+ response.getHttpDate(ses.expiry) + "; Path=/; SameSite=Strict; HttpOnly", true);
					authenticatedUsers.put(group + "." + session, ses);

					response.status = 200;
					response.message = "OK";
					return new AuthResult();
				} else {
					message = "Invalid credentials";
					response.status = 401;
					response.message = "Authorization required";
				}
			}
		}

		if (request.query.contains("?login=check&")) {
			request.query = request.query.replace("?login=check&", "?");
		} else if (request.query.contains("?login=check")) {
			request.query = request.query.replace("?login=check", "");
		} else if (request.query.contains("&login=check")) {
			request.query = request.query.replace("&login=check", "");
		}

		if (request.query.contains("?login=final&")) {
			request.query = request.query.replace("?login=final&", "?");
		} else if (request.query.contains("?login=final")) {
			request.query = request.query.replace("?login=final", "");
		} else if (request.query.contains("&login=final")) {
			request.query = request.query.replace("&login=final", "");
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

		if (!cookies.containsKey(group + ".session")
				|| !authenticatedUsers.containsKey(group + "." + cookies.get(group + ".session"))) {
			if (query.getOrDefault("logout", "false").equalsIgnoreCase("true")) {
				String q = "";
				for (String k : query.keySet()) {
					if (k.equals("servicename") || k.equals("login") || k.equals("logout"))
						continue;
					if (q.isEmpty())
						q += "?";
					else
						q += "&";
					q += URLEncoder.encode(k, "UTF-8");
					q += "=";
					q += URLEncoder.encode(query.get(k), "UTF-8");
				}

				response.status = 302;
				response.message = "File found";
				response.headers.put("Location", request.path + q);
				return new AuthResult();
			}

			response.status = 401;
			response.message = "Authorization required";

			displayMessages(response, request, group, message, buttonBackground, lblColor, file, submitProps, "");

			return new AuthResult();
		} else {
			if (query.getOrDefault("logout", "false").equalsIgnoreCase("true")) {
				authenticatedUsers.remove(group + "." + cookies.get(group + ".session"));
				response.setHeader("Set-Cookie", group + ".session=logout; Expires=" + response.getHttpDate(new Date())
						+ "; Path=/; SameSite=Strict; HttpOnly", true);

				String q = "";
				for (String k : query.keySet()) {
					if (k.equals("servicename") || k.equals("login") || k.equals("logout"))
						continue;
					if (q.isEmpty())
						q += "?";
					else
						q += "&";
					q += URLEncoder.encode(k, "UTF-8");
					q += "=";
					q += URLEncoder.encode(query.get(k), "UTF-8");
				}

				response.status = 302;
				response.message = "File found";
				response.headers.put("Location", request.path + q);
				return new AuthResult();
			}

			response.status = 200;
			response.message = "OK";
			authenticatedUsers.get(group + "." + cookies.get(group + ".session")).user.openSecureStorage();
			return authenticatedUsers.get(group + "." + cookies.get(group + ".session")).user;
		}
	}

	private void displayMessages(HttpResponse response, HttpRequest request, String group, String message,
			String buttonBackground, String lblColor, String file, String submitProps, String extra) {
		response.setContent("text/html", "<!DOCTYPE html>\r\n" + "<html>\r\n" + "<head>\r\n"
				+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\r\n" + "<style>\r\n"
				+ "body {font-family: Arial, Helvetica, sans-serif;}\r\n" + "\r\n" + "/* Full-width input fields */\r\n"
				+ "input[type=text], input[type=password] {\r\n" + "  width: 100%;\r\n" + "  padding: 12px 20px;\r\n"
				+ "  margin: 8px 0;\r\n" + "  display: inline-block;\r\n" + "  border: 1px solid #ccc;\r\n"
				+ "  box-sizing: border-box;\r\n" + "}\r\n" + "\r\n" + "/* Set a style for all buttons */\r\n"
				+ "button {\r\n" + "  background-color: " + buttonBackground + ";\r\n" + "  color: white;\r\n"
				+ "  padding: 14px 20px;\r\n" + "  margin: 8px 0;\r\n" + "  border: none;\r\n"
				+ "  cursor: pointer;\r\n" + "  width: 100%;\r\n" + "}\r\n" + "\r\n" + "button:hover {\r\n"
				+ "  opacity: 0.8;\r\n" + "}\r\n" + "\r\n" + "/* Center the image and position the close button */\r\n"
				+ ".imgcontainer {\r\n" + "  text-align: center;\r\n" + "  margin: 24px 0 12px 0;\r\n"
				+ "  position: relative;\r\n" + "}\r\n" + "\r\n" + ".container {\r\n" + "  padding: 16px;\r\n" + "}\r\n"
				+ "\r\n" + "span.error {\r\n" + "  color:" + lblColor + ";\r\n float: right;\r\n size: auto;\r\n"
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
				+ "/* The Close Button (x) */\r\n" + ".close {\r\n" + "  position: absolute;\r\n" + "  right: 25px;\r\n"
				+ "  top: 0;\r\n" + "  color: #000;\r\n" + "  font-size: 35px;\r\n" + "  font-weight: bold;\r\n"
				+ "}\r\n" + "\r\n" + ".close:hover,\r\n" + ".close:focus {\r\n" + "  color: red;\r\n"
				+ "  cursor: pointer;\r\n" + "}\r\n" + "\r\n" + "/* Add Zoom Animation */\r\n"
				+ (!message.isEmpty() ? ""
						: ".animate {\r\n" + "  -webkit-animation: animatezoom 1s;\r\n"
								+ "  animation: animatezoom 1s\r\n" + "}")
				+ "\r\n" + "\r\n" + "@-webkit-keyframes animatezoom {\r\n" + "  from {-webkit-transform: scale(0)} \r\n"
				+ "  to {-webkit-transform: scale(1)}\r\n" + "}\r\n" + "  \r\n" + "@keyframes animatezoom {\r\n"
				+ "  from {transform: scale(0)} \r\n" + "  to {transform: scale(1)}\r\n" + "}\r\n" + "\r\n"
				+ "/* Change styles for span and cancel button on extra small screens */\r\n"
				+ "@media screen and (max-width: 300px) {\r\n" + "  span.error {\r\n"
				+ "     color:red;\r\n    display: block;\r\n" + "     float: none;\r\n" + "  }\r\n"
				+ "  .cancelbtn {\r\n" + "     width: 100%;\r\n" + "  }\r\n" + "}\r\n" + "\r\n" + ".welcome {\r\n"
				+ "text-align: center\r\n" + "}\r\n" + "</style>\r\n" + "</head>\r\n" + "<body>\r\n" + "\r\n"
				+ "<script src=\"https://code.jquery.com/jquery-3.6.0.min.js\"></script>\r\n"
				+ "<form class=\"modal-content animate\" action=\"" + file + "?"
				+ (request.query == null || request.query.isEmpty() ? "" : request.query + "&")
				+ "login=check\" method=\"post\">\r\n"
				+ "	<h1 class=\"welcome\">Welcome, please log in to a valid account to access the requested resource.</h1>\r\n"
				+ "\r\n" + "    <div class=\"container\">\r\n" + "      <label for=\"user\"><b>Username</b></label>\r\n"
				+ "      <input type=\"text\" placeholder=\"Enter Username\" name=\"user\" required>\r\n" + "\r\n"
				+ "      <label for=\"pass\"><b>Password</b></label>\r\n"
				+ "      <input type=\"password\" placeholder=\"Enter Password\" name=\"pass\" required>\r\n"
				+ "        \r\n" + "      <button type=\"submit\"" + submitProps + ">Login</button>\r\n"
				+ "    </div>\r\n" + "\r\n" + "    <div class=\"container\" style=\"background-color:#f1f1f1\">\r\n"
				+ "      <span class=\"error\">" + message + "</span>\r\n"
				+ "		  <a class=\"credits\">Credits to W3Schools for the form.</a>\r\n" + "    </div>\r\n"
				+ "  </form>\r\n" + "\r\n" + "</body>\r\n" + "</html>\r\n" + extra + "\r\n");

	}

	private String genSessionKey() {
		return System.currentTimeMillis() + "-" + UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString()
				+ "-" + UUID.randomUUID().toString() + "-" + System.currentTimeMillis();
	}

	@Override
	public boolean check(String group, HttpRequest request, HttpResponse response) throws IOException {
		if (request.headers.containsKey("Authorization") || request.headers.containsKey("X-Use-HTTP-Authentication")) {
			return new DefaultAuthFrontend().check(group, request, response);
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

		return cookies.containsKey(group + ".session")
				&& authenticatedUsers.containsKey(group + "." + cookies.get(group + ".session"));
	}
}
