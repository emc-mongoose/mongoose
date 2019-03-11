package com.emc.mongoose.util;

import java.io.IOException;

/** Created by kurila on 06.06.17. */
public interface DirWithManyFilesDeleter {

	static void deleteExternal(final String path) throws IOException, InterruptedException {
		final Process p = Runtime.getRuntime().exec(new String[]{"rm", "-rf", path
		});
		p.waitFor();
	}
}
