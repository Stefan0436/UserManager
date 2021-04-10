package org.asf.connective.usermanager.implementation.commands.admin;

import java.io.IOException;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;

public class ListProductGroups extends AbstractAdminCommand {

	@Override
	public String id() {
		return "list-product-groups";
	}

	@Override
	public IAdminCommand newInstance() {
		return new ListProductGroups();
	}

	@Override
	public void run() {
		setOutput("Product groups:");
		for (String group : UserManagerModule.getAllowedGroups()) {
			if (UserManagerModule.isProductGroup(group)) {
				try {
					setOutput(group, "users: " + UserManagerModule.getAuthBackend().getUsers(group).length);
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void prepare() {
	}

}
