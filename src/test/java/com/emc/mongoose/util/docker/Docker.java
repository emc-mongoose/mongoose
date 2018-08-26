package com.emc.mongoose.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public interface Docker {

	String DEFAULT_IMAGE_VERSION = "latest";
	DockerClient CLIENT = DockerClientBuilder.getInstance().build();

	interface Container
	extends AsyncRunnable  {

		long DEFAULT_MEMORY_LIMIT = SizeInBytes.toFixedSize("1GB");

		int exitStatusCode();

		String stdOutContent();

		String stdErrContent();
	}
}
