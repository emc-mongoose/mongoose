package com.emc.mongoose.util;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

/**
 Created by kurila on 11.07.16.
 */
public abstract class PathsUtil {

	public static URI getBaseUriForClass(final Class<?> cls)
	throws URISyntaxException {
		return cls.getProtectionDomain().getCodeSource().getLocation().toURI();
	}

	// http://stackoverflow.com/a/29665447
	public static String getBasePathForClass(final Class<?> cls) {
		try {
			String basePath;
			final File clsFile = new File(getBaseUriForClass(cls).getPath());
			if(
				clsFile.isFile() ||
					clsFile.getPath().endsWith(".jar") ||
					clsFile.getPath().endsWith(".zip")
				) {
				basePath = clsFile.getParent();
			} else {
				basePath = clsFile.getPath();
			}
			// bandage for eclipse
			if(
				basePath.endsWith(File.separator + "lib") ||
					basePath.endsWith(File.separator + "bin") ||
					basePath.endsWith("bin" + File.separator) ||
					basePath.endsWith("lib" + File.separator)
				) {
				basePath = basePath.substring(0, basePath.length() - 4);
			}
			// bandage for netbeans
			if (basePath.endsWith(File.separator + "build" + File.separator + "classes")) {
				basePath = basePath.substring(0, basePath.length() - 14);
			}
			// bandage for gradle
			if (basePath.endsWith(File.separator + "build" + File.separator + "classes" + File.separator + "main")) {
				basePath = basePath.substring(0, basePath.length() - 19);
			}
			// bandage for idea
			if (basePath.endsWith(File.separator + "build" + File.separator + "resources" + File.separator + "common")) {
				basePath = basePath.substring(0, basePath.length() - 23);
			}
			// final fix
			if(!basePath.endsWith(File.separator)) {
				basePath = basePath + File.separator;
			}
			return basePath;
		} catch(final URISyntaxException e) {
			throw new RuntimeException("Cannot figure out base path for class: " + cls.getName());
		}
	}

	private static String DIR_WORKING = null;
	public static String getWorkingDir() {
		if(DIR_WORKING == null) {
			DIR_WORKING = getBasePathForClass(PathsUtil.class);
		}
		return DIR_WORKING;
	}
}
