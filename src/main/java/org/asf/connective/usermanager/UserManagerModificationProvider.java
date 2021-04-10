package org.asf.connective.usermanager;

import java.io.IOException;

import org.asf.rats.http.IAutoContextModificationProvider;
import org.asf.rats.http.ProviderContextFactory;

// This class allows RaTs! to use the module components, it allows
// for modifying any context created by the IAutoContextBuilders used by RaTs.
public class UserManagerModificationProvider implements IAutoContextModificationProvider {

	@Override
	public void accept(ProviderContextFactory arg0) {
		if (!UserManagerModificationManager.hasBeenPrepared()) {
			try {
				UserManagerModificationManager.prepareModifications();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		UserManagerModificationManager.appy(arg0);
	}

}
