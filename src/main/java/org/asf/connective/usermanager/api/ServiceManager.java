package org.asf.connective.usermanager.api;

import java.io.IOException;
import java.util.HashMap;

import org.asf.rats.Memory;
import org.asf.rats.ModuleBasedConfiguration;

/**
 * 
 * Simple registration system for IAuthService instances
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServiceManager {
	/**
	 * Registers the given service (does not add it to the services confguration)
	 * 
	 * @param service Service to register
	 */
	public static void registerService(IAuthService service) {
		Memory.getInstance().getOrCreate("auth.service." + service.path()).append(service);
	}

	/**
	 * Adds the service to the service configuration, should only be done if the
	 * owning module's configuration has not been created yet.
	 * 
	 * @param service Service to add
	 */
	@SuppressWarnings("unchecked")
	public static void addToConfiguration(IAuthService service) {
		if (((HashMap<String, String>) Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class).modules.get("UserManager-AuthServices")) == null) {
			Memory.getInstance().get("memory.modules.shared.config").getValue(ModuleBasedConfiguration.class).modules
					.put("UserManager-AuthServices", new HashMap<String, String>());
		}
		((HashMap<String, String>) Memory.getInstance().get("memory.modules.shared.config")
				.getValue(ModuleBasedConfiguration.class).modules.get("UserManager-AuthServices")).put(service.name(),
						service.path());
	}

	public static void saveConfigurationAndReload() throws IOException {
		Memory.getInstance().get("memory.modules.shared.config").getValue(ModuleBasedConfiguration.class).writeAll();
		for (Runnable run : Memory.getInstance().get("bootstrap.reload").getValues(Runnable.class)) {
			run.run();
		}
	}
}
