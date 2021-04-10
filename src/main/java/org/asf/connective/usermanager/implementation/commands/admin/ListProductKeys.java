package org.asf.connective.usermanager.implementation.commands.admin;

import java.io.IOException;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;
import org.asf.connective.usermanager.configs.physical.ProductKeyConfig;

public class ListProductKeys extends AbstractAdminCommand {

	@Override
	public String id() {
		return "list-product-keys";
	}

	@Override
	public IAdminCommand newInstance() {
		return new ListProductKeys();
	}

	@Override
	public void run() {
		String group = getValue("group");
		if (!UserManagerModule.isProductGroup(group)) {
			setOutput("Not a product group");
			setResult(false);
			return;
		}
		if (!UserManagerModule.getAuthBackend().validateGroupname(group)) {
			setOutput("Malformed group name");
			setResult(false);
			return;
		}

		setOutput("Valid product key(s) for " + group + ":");
		for (String key : UserManagerModule.getValidProductKeys(group)) {
			try {
				ProductKeyConfig keyConf = UserManagerModule.getProductKey(group, key);
				setOutput(key, "expires: " + keyConf.expires + ", remaining uses: "
						+ (keyConf.remainingUses == -1 ? "unlimited" : keyConf.remainingUses));
			} catch (IOException e) {
			}
		}
	}

	@Override
	public void prepare() {
		this.registerArgument("group");
	}

}
