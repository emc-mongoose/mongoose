package com.emc.mongoose.env;

import com.github.akurilov.confuse.Config;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public interface Extension {

	static List<Extension> load(final File dirExt) {

		final Logger log = Logger.getLogger(Extension.class.getSimpleName());
		final URLClassLoader extClsLoader;
		if(!dirExt.exists() || !dirExt.isDirectory()) {
			log.warning("No \"" + dirExt.getAbsolutePath() + "\" directory, loaded no extensions");
			extClsLoader = new URLClassLoader(new URL[] {});
		} else {
			final File[] extFiles = dirExt.listFiles();
			if(extFiles == null) {
				log.warning(
					"Failed to load the contents of the \"" + dirExt.getAbsolutePath()
						+ "\" directory, loaded no extensions"
				);
				extClsLoader = new URLClassLoader(new URL[] {});
			} else {
				final URL[] extFileUrls = new URL[extFiles.length];
				final JarFile[] extFileJars = new JarFile[extFiles.length];
				for(int i = 0; i < extFiles.length; i ++) {
					try {
						extFileUrls[i] = extFiles[i].toURI().toURL();
						log.config("Loading the extension from the file: \"" + extFiles[i] + "\"");
						try {
							extFileJars[i] = new JarFile(extFiles[i]);
						} catch(final IOException e) {
							log.warning(
								"Failed to load the file \"" + extFiles[i]
									+ "\", expected a valid JAR/ZIP file"
							);
						}
					} catch(final MalformedURLException e) {
						throw new AssertionError(e);
					}
				}
				extClsLoader = new URLClassLoader(extFileUrls, ClassLoader.getSystemClassLoader());
			}
		}

		final List<Extension> extensions = new ArrayList<>();
		for(final Extension extension: ServiceLoader.load(Extension.class, extClsLoader)) {
			extension.classLoader(extClsLoader);
			extensions.add(extension);
		}
		return extensions;
	}

	String id();

	ClassLoader classLoader();

	void classLoader(final ClassLoader clsLoader);

	void install(final Path appHomePath);

	Config defaults(final Path appHomePath);
}
