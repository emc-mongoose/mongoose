package com.emc.mongoose.base.env;

import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.logging.Logger;

/**
 * Extension is an @see {@link Installable} with the configuration and the
 * configuration schema.
 */
public interface Extension extends Installable {

	Logger LOG = Logger.getLogger(Extension.class.getSimpleName());

	static List<Extension> load(final ClassLoader extClsLoader) {
		final List<Extension> extensions = new ArrayList<>();
		for (final Extension extension : ServiceLoader.load(Extension.class, extClsLoader)) {
			extensions.add(extension);
		}
		return extensions;
	}

	static URLClassLoader extClassLoader(final File dirExt) {
		final URLClassLoader extClsLoader;
		if (!dirExt.exists() || !dirExt.isDirectory()) {
			LOG.warning("No \"" + dirExt.getAbsolutePath() + "\" directory, loaded no extensions");
			extClsLoader = new URLClassLoader(new URL[]{});
		} else {
			final File[] extFiles = dirExt.listFiles();
			if (extFiles == null) {
				LOG.warning(
								"Failed to load the contents of the \""
												+ dirExt.getAbsolutePath()
												+ "\" directory, loaded no extensions");
				extClsLoader = new URLClassLoader(new URL[]{});
			} else {
				final URL[] extFileUrls = Arrays.stream(extFiles)
								.filter(Extension::isJarFile)
								.map(Extension::fileToUrl)
								.filter(Objects::nonNull)
								.toArray(URL[]::new);
				extClsLoader = new URLClassLoader(extFileUrls, ClassLoader.getSystemClassLoader());
			}
		}
		return extClsLoader;
	}

	static boolean isJarFile(final File f) {
		try {
			new JarFile(f);
		} catch (final Exception e) {
			LOG.warning("Failed to load the file \"" + f + "\", expected a valid JAR/ZIP file");
			return false;
		}
		return true;
	}

	static URL fileToUrl(final File f) {
		try {
			return f.toURI().toURL();
		} catch (final MalformedURLException e) {
			LOG.severe(e.toString());
		}
		return null;
	}

	String id();

	Config defaults(final Path appHomePath);

	SchemaProvider schemaProvider();
}
