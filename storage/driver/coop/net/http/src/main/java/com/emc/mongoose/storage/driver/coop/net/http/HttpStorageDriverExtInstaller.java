package com.emc.mongoose.storage.driver.coop.net.http;

import com.emc.mongoose.env.JarResourcesInstaller;

import java.io.InputStream;

public final class HttpStorageDriverExtInstaller
extends JarResourcesInstaller {

	@Override
	protected InputStream resourceStream(final String resPath) {
		return HttpStorageDriverExtInstaller.class.getResourceAsStream(resPath);
	}
}
