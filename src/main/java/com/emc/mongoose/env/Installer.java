package com.emc.mongoose.env;

import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.function.Consumer;

public interface Installer
extends Consumer<Path> {

	String RESOURCES_TO_INSTALL_PREFIX = "install";

	static void installExtensions(final Path appHomePath, final ClassLoader clsLoader) {
		ServiceLoader
			.load(Installer.class, clsLoader)
			.forEach(installer -> installer.accept(appHomePath));
	}
}
