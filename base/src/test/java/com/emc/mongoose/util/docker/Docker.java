package com.emc.mongoose.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public interface Docker {

	String DEFAULT_IMAGE_VERSION = "latest";
	DockerClient CLIENT = DockerClientBuilder.getInstance().build();

	interface Container extends AsyncRunnable {

		long DEFAULT_MEMORY_LIMIT = SizeInBytes.toFixedSize("2GB");

		int exitStatusCode();

		String stdOutContent();

		String stdErrContent();

		static String serviceHost() {
			final String serviceHost = System.getenv("SERVICE_HOST");
			if (null == serviceHost || serviceHost.isEmpty()) {
				return "127.0.0.1";
			} else {
				return serviceHost;
			}
		}
	}
}
