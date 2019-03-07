package com.emc.mongoose.base.env;

import java.nio.file.Path;

/** Installable is a thing that accepts the destination path to install some resources into it */
public interface Installable {

	void install(final Path dstPath);
}
