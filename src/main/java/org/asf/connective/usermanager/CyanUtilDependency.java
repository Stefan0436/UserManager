package org.asf.connective.usermanager;

import org.asf.connective.standalone.IModuleMavenDependencyProvider;

public class CyanUtilDependency implements IModuleMavenDependencyProvider {

	@Override
	public String group() {
		return "org.asf.cyan";
	}

	@Override
	public String name() {
		return "CyanUtil";
	}

	@Override
	public String version() {
		return "1.0.0.A5";
	}

}
