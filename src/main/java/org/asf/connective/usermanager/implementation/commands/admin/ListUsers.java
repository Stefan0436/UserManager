package org.asf.connective.usermanager.implementation.commands.admin;

import java.io.IOException;
import java.util.stream.Stream;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;

public class ListUsers extends AbstractAdminCommand {

	@Override
	public String id() {
		return "list-users";
	}

	@Override
	public IAdminCommand newInstance() {
		return new ListUsers();
	}

	@Override
	public void run() {
		String group = getValue("group");
		if (!UserManagerModule.getAuthBackend().validateGroupname(group)) {
			setOutput("Malformed group name");
			setResult(false);
			return;
		}
		if (!Stream.of(UserManagerModule.getAllowedGroups()).anyMatch(t -> t.equals(group))) {
			setOutput("Group not recognized as an allowed group");
			setResult(false);
			return;
		}

		setOutput("Users in group " + group + ":");
		try {
			for (String user : UserManagerModule.getAuthBackend().getUsers(group)) {
				setOutput(user, null);
			}
		} catch (IOException e) {
		}
	}

	@Override
	public void prepare() {
		this.registerArgument("group");
	}

}
