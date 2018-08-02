package com.emc.mongoose.env;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.function.Consumer;

@FunctionalInterface
public interface Installer
extends Consumer<Path> {
}
