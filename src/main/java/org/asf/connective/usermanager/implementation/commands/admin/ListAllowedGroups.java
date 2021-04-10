package org.asf.connective.usermanager.implementation.commands.admin;

import java.io.IOException;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;

public class ListAllowedGroups extends AbstractAdminCommand {

	@Override
	public String id() {
		return "list-allowed-groups";
	}

	@Override
	public IAdminCommand newInstance() {
		return new ListAllowedGroups();
	}

	@Override
	public void run() {
		setOutput("All allowed groups:");
		for (String group : UserManagerModule.getAllowedGroups()) {
			if (UserManagerModule.isProductGroup(group)) {
				try {
					setOutput(group,
							"type: product, users: " + UserManagerModule.getAuthBackend().getUsers(group).length);
				} catch (IOException e) {
				}
			} else if (UserManagerModule.getAdminGroup().equals(group)) {
				try {
					setOutput(group,
							"type: admin, users: " + UserManagerModule.getAuthBackend().getUsers(group).length);
				} catch (IOException e) {
				}
			} else {
				try {
					setOutput(group,
							"type: normal, users: " + UserManagerModule.getAuthBackend().getUsers(group).length);
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void prepare() {
	}

}
