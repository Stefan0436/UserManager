package org.asf.connective.usermanager.configs.physical;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import org.asf.connective.usermanager.UserManagerModule;
import org.asf.cyan.api.config.Configuration;

public class ProductKeyConfig extends Configuration<ProductKeyConfig> {

	public ProductKeyConfig(int expiryDays) {
		if (expiryDays == -1) {
			expires = "never";
		} else {
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, expiryDays);
			expires = UserManagerModule.getDateString(cal.getTime());
		}
	}

	public ProductKeyConfig() {
		expires = "never";
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String key = null;
	public String expires;
	public int remainingUses = -1;

	public ProductKeyConfig setInfo(String productKey) {
		key = productKey;
		return this;
	}

	public boolean isStillValid() {
		if (expires.equals("never"))
			return true;

		try {
			Date expiry = UserManagerModule.parseDateString(expires);
			Date date =  new Date();
			boolean after = date.after(expiry);
			return !after;
		} catch (ParseException e) {
			return true;
		}
	}

}
