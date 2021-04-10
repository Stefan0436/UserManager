package org.asf.connective.usermanager.implementation.commands.admin;

import java.io.IOException;
import java.math.BigInteger;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.connective.usermanager.api.AbstractAdminCommand;
import org.asf.connective.usermanager.api.IAdminCommand;
import org.asf.connective.usermanager.configs.physical.ProductKeyConfig;

public class CreateProductKeys extends AbstractAdminCommand {

	@Override
	public String id() {
		return "create-product-keys";
	}

	@Override
	public IAdminCommand newInstance() {
		return new CreateProductKeys();
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

		String countArg = getValue("count");
		if (countArg == null)
			countArg = "1";

		String expArg = getValue("expirydays");
		if (expArg == null)
			expArg = "30";
		int expiryDays = Integer.valueOf(expArg);

		String maxArg = getValue("maxuses");
		if (maxArg == null)
			maxArg = "5";
		int maxUses = Integer.valueOf(maxArg);

		BigInteger count = new BigInteger(countArg);
		BigInteger current = BigInteger.ZERO;
		while (count.compareTo(current) == 1) {
			try {
				ProductKeyConfig key = UserManagerModule.generateUnusedProductKey(group, maxUses, expiryDays);
				setOutput(key.key, "expires: " + key.expires + ", max uses: "
						+ (key.remainingUses == -1 ? "unlimited" : key.remainingUses));
			} catch (IOException e) {
				setOutput("Error occured: " + e.getClass().getTypeName() + ": " + e.getMessage()
						+ ",\nthe following keys were created:");
				setResult(false);
				return;
			}
			current = current.add(BigInteger.ONE);
		}

		setOutput("Generated " + count + " product key" + (count.equals(BigInteger.ONE) ? "" : "s") + " for " + group
				+ ":");
	}

	@Override
	public void prepare() {
		this.registerArgument("group");
		this.registerOptionalArgument("count");
		this.registerOptionalArgument("expirydays");
		this.registerOptionalArgument("maxuses");
	}

}
