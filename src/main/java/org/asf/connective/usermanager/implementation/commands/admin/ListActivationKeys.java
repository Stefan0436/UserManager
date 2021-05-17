package org.asf.connective.usermanager.implementation.commands.admin;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;
import org.asf.connective.usermanager.configs.physical.ActivationKeyConfig;

public class ListActivationKeys extends AbstractAdminCommand {

	@Override
	public String id() {
		return "list-activation-keys";
	}

	@Override
	public IAdminCommand newInstance() {
		return new ListActivationKeys();
	}

	@Override
	public void run() {
		String group = getValue("group");
		if (!UserManagerModule.getAuthBackend().validateGroupname(group)) {
			setOutput("Malformed group name");
			setResult(false);
			return;
		}

		setOutput("Valid activation key(s) for " + group + ":");
		for (ActivationKeyConfig key : UserManagerModule.getActivationKeys()) {
			if (key.group.equals(group)) {
				setOutput(key.key,
						"user: " + key.username + ", expires: " + key.expiryDate + ", cancel key: " + key.cancelKey);
			}
		}
	}

	@Override
	public void prepare() {
		this.registerArgument("group");
	}

}
