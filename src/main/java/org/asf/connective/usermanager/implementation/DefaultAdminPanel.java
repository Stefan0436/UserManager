package org.asf.connective.usermanager.implementation;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.asf.connective.usermanager.api.AdminPanelFrontend;
import org.asf.connective.usermanager.api.ArgumentSpecification;
import org.asf.connective.usermanager.api.IAdminCommand;
import org.asf.connective.usermanager.util.ParsingUtil;

public class DefaultAdminPanel extends AdminPanelFrontend {
	public static void assign() {
		if (implementation == null)
			AdminPanelFrontend.assignMainImplementation(new DefaultAdminPanel());
	}

	@Override
	protected AdminPanelFrontend newInstance() {
		return new DefaultAdminPanel();
	}

	@Override
	protected void runFrontend() {
		if (getRequest().query != null) {
			HashMap<String, String> query = ParsingUtil.parseQuery(getRequest().query);
			if (query.containsKey("command")) {
				String command = query.get("command");
				for (IAdminCommand cmd : getCommands()) {
					if (cmd.id().equalsIgnoreCase(command)) {
						String[] args = new String[query.size() - 1];

						int i = 0;
						for (String arg : query.keySet())
							if (!arg.equals("command"))
								args[i++] = arg;

						IAdminCommand newcmd = runCommand(cmd, () -> args, (key) -> query.get(key));
						if (newcmd == null) {
							StringBuilder arguments = new StringBuilder();
							for (ArgumentSpecification spec : cmd.specification()) {
								if (spec.isRequired())
									arguments.append("*- ");
								else
									arguments.append(" - ");

								if (!spec.getType().getTypeName().equals(String.class.getTypeName())) {
									arguments.append("[" + spec.getType().getSimpleName() + "] ");
								}

								arguments.append(spec.getName());
								arguments.append("\n");
							}

							getResponse().setContent("text/plain",
									"Syntax error, known arguments:\n" + arguments.toString());
							return;
						}

						Map<String, String> outp = newcmd.output();
						if (!outp.isEmpty()) {
							StringBuilder output = new StringBuilder();
							boolean contained = false;
							if (outp.containsKey("")) {
								output.append(" - ").append(outp.get("")).append("\n");
								contained = true;
							}
							for (String k : outp.keySet()) {
								if (!k.isEmpty()) {
									if (contained) {
										output.append("\n");
										contained = false;
									}
									output.append(" - ").append(k);
									if (outp.get(k) != null) {
										output.append(" : ").append(outp.get(k)).append("\n");
									}
								}
							}
							getResponse().setContent("text/plain", output.toString());
						}

						return;
					}
				}
			}
		}

		getResponse().status = 400;
		getResponse().message = "Bad request";

		StringBuilder commands = new StringBuilder();
		for (IAdminCommand cmd : getCommands()) {
			long count = Stream.of(cmd.specification()).filter(t -> t.isRequired()).count();
			commands.append(" - ").append(cmd.id()).append(" - ").append(count).append(" argument");
			if (count != 1)
				commands.append("s");
			commands.append(".\n");
		}
		getResponse().setContent("text/plain",
				"Please specify a valid command in the query string.\nList of commands:\n" + commands);
	}
}
