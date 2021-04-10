package org.asf.connective.usermanager.configs.physical;

import java.io.File;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Exclude;

public class ActivationKeyConfig extends Configuration<ActivationKeyConfig> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String key = null;
	public String group = null;

	public String username = null;
	public String password = null;
	public String cancelKey = null;

	public String expiryDate = null;

	public ProductKeyConfig productKey = null;
	
	@Exclude
	public File file;

	public ActivationKeyConfig setInfo(String group, File keyFile) {
		this.group = group;
		this.key = keyFile.getName().substring(0, keyFile.getName().lastIndexOf(".ccfg"));
		return this;
	}

}
