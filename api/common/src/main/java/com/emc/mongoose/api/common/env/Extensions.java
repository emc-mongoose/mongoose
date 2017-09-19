package com.emc.mongoose.api.common.env;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Logger;

public abstract class Extensions {

	public static final String DIR_EXT = PathUtil.getBaseDir() + File.separator + "ext";
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
				for(int i = 0; i < extFiles.length; i ++) {
					try {
						extFileUrls[i] = extFiles[i].toURI().toURL();
						LOG.config("Loading the extension from the file: \"" + extFiles[i] + "\"");
					} catch(final MalformedURLException e) {
						throw new AssertionError(e);
					}
				}
				CLS_LOADER = new URLClassLoader(extFileUrls);
				LOG.info("Loaded " + extFileUrls.length + " extensions");
			}
		}
	}
}
