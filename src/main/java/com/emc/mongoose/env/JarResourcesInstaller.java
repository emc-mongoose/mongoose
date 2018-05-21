package com.emc.mongoose.env;

import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class JarResourcesInstaller
implements Installer {

	@Override
	public void accept(final Path appHomePath) {

		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}

		final URL rootResUrl = this.getClass().getResource("");
		if(rootResUrl == null) {
			throw new IllegalStateException("Failed to get the root resources URL");
		}
		if(!"jar".equals(rootResUrl.getProtocol())) {
			throw new IllegalStateException(
				"Root resources URL doesn't point to the jar file: " + rootResUrl
			);
		}

		final String jarPath;
		try {
			// cut the remaining "file:" prefix
			final String t = new URL(rootResUrl.getPath()).getPath();
			if(t.isEmpty()) {
				throw new IllegalStateException("Root resources path is empty");
			}
			// cut the suffix with the internal jar path
			final int i = t.indexOf('!');
			jarPath = t.substring(0, i);
		} catch(final MalformedURLException e) {
			throw new IllegalStateException(e);
		}

		Loggers.MSG.info("Try to install resources from {}...", jarPath);
		try(final ZipFile jarFile = new ZipFile(jarPath)) {
			jarFile
				.stream()
				.filter(((Predicate<ZipEntry>) ZipEntry::isDirectory).negate())
				.map(ZipEntry::getName)
				.filter(JarResourcesInstaller::isResourceToInstall)
				.forEach(srcFilePath -> installResourcesFile(appHomePath, srcFilePath));
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
		Loggers.MSG.info("Install hook finished");
	}

	private static boolean isResourceToInstall(final String relPath) {
		return relPath.startsWith(RESOURCES_TO_INSTALL_PREFIX);
	}

	private void installResourcesFile(final Path appHomePath, final String srcFilePath) {
		final Path dstPath = Paths.get(
			appHomePath.toString(), srcFilePath.substring(RESOURCES_TO_INSTALL_PREFIX.length() + 1)
		);
		if(dstPath.toFile().exists()) {
			Loggers.MSG.debug("The file {} already exists, skipping", dstPath);
			return;
		}
		dstPath.getParent().toFile().mkdirs();
		try(final InputStream srcFileInput = resourceStream(File.separator + srcFilePath)) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			Loggers.MSG.debug("The file {} installed ({})", dstPath, copiedBytesCount);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to install file {}", dstPath);
		}
	}

	protected abstract InputStream resourceStream(final String resPath);
}
