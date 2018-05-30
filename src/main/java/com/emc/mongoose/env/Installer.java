package com.emc.mongoose.env;

import java.nio.file.Path;
import java.util.function.Consumer;

@FunctionalInterface
public interface Installer
extends Consumer<Path> {

	String RESOURCES_TO_INSTALL_PREFIX = "install";
}
