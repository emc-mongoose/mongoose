package com.emc.mongoose.common.env;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

/**
 Created by kurila on 03.11.16.
 */
public interface PathUtil {
	
	static String getBaseDir() {
		final URL baseUrl = PathUtil.class.getProtectionDomain().getCodeSource().getLocation();
		try {
			return new File(baseUrl.toURI()).getParent();
		} catch(final URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}
}
