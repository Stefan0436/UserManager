package org.asf.connective.usermanager.util;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Parser utility class - parsers for basic commands and http queries.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ParsingUtil {

	/**
	 * Parses the given space-separated command/arguments into an arraylist,
	 * supports quotes and escaping.
	 * 
	 * @param cmd Input to parse
	 * @return ArrayList representing the command.
	 */
	public static ArrayList<String> parseCommand(String cmd) {
		ArrayList<String> args3 = new ArrayList<String>();
		char[] argarray = cmd.toCharArray();
		boolean ignorespaces = false;
		String last = "";
		int i = 0;
		for (char c : argarray) {
			if (c == '"' && (i == 0 || argarray[i - 1] != '\\')) {
				if (ignorespaces)
					ignorespaces = false;
				else
					ignorespaces = true;
			} else if (c == ' ' && !ignorespaces && (i == 0 || argarray[i - 1] != '\\')) {
				args3.add(last);
				last = "";
			} else if (c != '\\' || (i + 1 < argarray.length && argarray[i + 1] != '"'
					&& (argarray[i + 1] != ' ' || ignorespaces))) {
				last += c;
			}

			i++;
		}

		if (last == "" == false)
			args3.add(last);

		return args3;
	}

	/**
	 * Parses a given query into a HashMap
	 * 
	 * @param query Query string
	 * @return HashMap instance with the query values
	 */
	public static HashMap<String, String> parseQuery(String query) {

		HashMap<String, String> map = new HashMap<String, String>();

		String key = "";
		String value = "";
		boolean isKey = true;

		for (int i = 0; i < query.length(); i++) {
			char ch = query.charAt(i);
			if (ch == '&' || ch == '?') {
				if (isKey && !key.isEmpty()) {
					map.put(key, "");
					key = "";
				} else if (!isKey && !key.isEmpty()) {
					try {
						map.put(key, URLDecoder.decode(value, "UTF-8"));
					} catch (UnsupportedEncodingException e) {
						map.put(key, value);
					}
					isKey = true;
					key = "";
					value = "";
				}
			} else if (ch == '=') {
				isKey = !isKey;
			} else {
				if (isKey) {
					key += ch;
				} else {
					value += ch;
				}
			}
		}
		if (!key.isEmpty() || !value.isEmpty()) {
			try {
				map.put(key, URLDecoder.decode(value, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				map.put(key, value);
			}
		}

		return map;
	}
}