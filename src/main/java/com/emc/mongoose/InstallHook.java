package com.emc.mongoose;


import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.Constants.PATH_DEFAULTS;
import static com.emc.mongoose.Constants.USER_HOME;
import com.emc.mongoose.config.ConfigUtil;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class InstallHook
implements Runnable {

	private static final String RESOURCES_TO_INSTALL_PREFIX = "install";

	private final Path appHomePath;
	private final Config bundledDefaults;

	public InstallHook()
	throws IllegalStateException, InvalidPathException  {
		final URL defaultConfigUrl = getClass().getResource(
			File.separator + RESOURCES_TO_INSTALL_PREFIX + File.separator + PATH_DEFAULTS
		);
		if(defaultConfigUrl == null) {
			throw new IllegalStateException("No bundled default config found");
		}
		try {
			final Map<String, Object> schema = SchemaProvider.resolveAndReduce(
				APP_NAME, getClass().getClassLoader()
			);
			bundledDefaults = ConfigUtil.loadConfig(defaultConfigUrl, schema);
		} catch(final Exception e) {
			throw new IllegalStateException(
				"Failed to load the bundled default config from the resources", e
			);
		}
		final String appVersion = bundledDefaults.stringVal("version");
		System.out.println(APP_NAME + " v " + appVersion);
		appHomePath = Paths.get(USER_HOME, "." + APP_NAME, appVersion);
		try {
			Files.createDirectories(appHomePath);
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}

	public final Path appHomePath() {
		return appHomePath;
	}

	public final Config bundledDefaults() {
		return bundledDefaults;
	}

	public final void run() {

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
				.filter(InstallHook::isResourceToInstall)
				.forEach(this::installResourcesFile);
		} catch(final IOException e) {
			throw new IllegalStateException(e);
		}
		Loggers.MSG.info("Install hook finished");
	}

	private static boolean isResourceToInstall(final String relPath) {
		return relPath.startsWith(RESOURCES_TO_INSTALL_PREFIX);
	}

	private void installResourcesFile(final String srcFilePath) {
		final Path dstPath = Paths.get(
			appHomePath.toString(), srcFilePath.substring(RESOURCES_TO_INSTALL_PREFIX.length() + 1)
		);
		if(dstPath.toFile().exists()) {
			Loggers.MSG.debug("The file {} already exists, skipping", dstPath);
			return;
		}
		dstPath.getParent().toFile().mkdirs();
		try(
			final InputStream
				srcFileInput = InstallHook.class.getResourceAsStream(File.separator + srcFilePath)
		) {
			final long copiedBytesCount = Files.copy(srcFileInput, dstPath);
			Loggers.MSG.debug("The file {} installed ({})", dstPath, copiedBytesCount);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to install file {}", dstPath);
		}
	}
}
