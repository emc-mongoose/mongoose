package com.emc.mongoose;

import com.emc.mongoose.logging.LogUtil;

public final class Main {

	public static void main(final String... args) {
		final InstallHook installHook = new InstallHook();
		LogUtil.init(installHook.appHomePath().toString());
		installHook.run();
	}
}
