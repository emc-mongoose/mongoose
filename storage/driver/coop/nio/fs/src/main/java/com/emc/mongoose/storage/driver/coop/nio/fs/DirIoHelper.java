package com.emc.mongoose.storage.driver.coop.nio.fs;

import java.io.File;

public interface DirIoHelper {

	static File createParentDir(final String parentPath) {
		try {
			final File parentDir = FsConstants.FS.getPath(parentPath).toFile();
			if (!parentDir.exists()) {
				parentDir.mkdirs();
			}
			return parentDir;
		} catch (final Exception e) {
			return null;
		}
	}
}
