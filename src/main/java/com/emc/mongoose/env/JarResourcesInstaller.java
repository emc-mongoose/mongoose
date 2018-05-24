package com.emc.mongoose.env;

import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public abstract class JarResourcesInstaller
implements Installer {

	@Override
	public void accept(final Path appHomePath) {
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
		resourceFilesToInstall().forEach(resFile -> installResourcesFile(appHomePath, resFile));
		Loggers.MSG.debug("Installer finished: \"{}\"", getClass().getCanonicalName());
	}

	private void installResourcesFile(final Path appHomePath, final String srcFilePath) {
		final Path dstPath = Paths.get(appHomePath.toString(), srcFilePath);
		if(dstPath.toFile().exists()) {
			Loggers.MSG.debug("The file {} already exists, skipping", dstPath);
			return;
		}
		dstPath.getParent().toFile().mkdirs();
		try(final InputStream srcFileInput = resourceStream(srcFilePath)) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			Loggers.MSG.debug("The file {} installed ({})", dstPath, copiedBytesCount);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to install file {}", dstPath);
		}
	}

	protected InputStream resourceStream(final String resPath) {
		return getClass().getResourceAsStream(File.separator + resPath);
	}

	protected abstract List<String> resourceFilesToInstall();
}
