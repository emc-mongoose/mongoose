package com.emc.mongoose.api.common.env;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;
import java.util.logging.Logger;

import static com.emc.mongoose.api.common.env.PathUtil.BASE_DIR;

public abstract class Extensions {

	public static final String DIR_EXT = BASE_DIR + File.separator + "ext";
	public static final URLClassLoader CLS_LOADER;

	private static final Logger LOG = Logger.getLogger(Extensions.class.getSimpleName());

	static {

		final File dirExt = new File(DIR_EXT);

		if(!dirExt.exists() || !dirExt.isDirectory()) {

			CLS_LOADER = new URLClassLoader(new URL[] {});
			LOG.warning("No \"" + dirExt.getAbsolutePath() + "\" directory, loaded no extensions");

		} else {

			final File[] extFiles = dirExt.listFiles();

			if(extFiles == null) {
				CLS_LOADER = new URLClassLoader(new URL[] {});
				LOG.warning(
					"Failed to load the contents of the \"" + dirExt.getAbsolutePath()
						+ "\" directory, loaded no extensions"
				);
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

				CLS_LOADER = new URLClassLoader(extFileUrls, ClassLoader.getSystemClassLoader());
			}
		}
	}
}
