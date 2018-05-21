package com.emc.mongoose.env;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.JarFile;
import java.util.logging.Logger;

public abstract class Extensions {

	private static final Logger LOG = Logger.getLogger(Extensions.class.getSimpleName());

	public static URLClassLoader extClassLoader(final Path appHomePath) {

		final URLClassLoader extClsLoader;

		final File dirExt = Paths.get(appHomePath.toString(), "ext").toFile();

		if(!dirExt.exists() || !dirExt.isDirectory()) {

			LOG.warning("No \"" + dirExt.getAbsolutePath() + "\" directory, loaded no extensions");
			extClsLoader = new URLClassLoader(new URL[] {});

		} else {

			final File[] extFiles = dirExt.listFiles();

			if(extFiles == null) {

				LOG.warning(
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
						LOG.config("Loading the extension from the file: \"" + extFiles[i] + "\"");
						try {
							extFileJars[i] = new JarFile(extFiles[i]);
						} catch(final IOException e) {
							LOG.warning(
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

		return extClsLoader;
	}
}
