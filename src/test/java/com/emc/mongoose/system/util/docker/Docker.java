package com.emc.mongoose.system.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DockerClientBuilder;

public interface Docker {

	String DEFAULT_IMAGE_VERSION = "latest";
	DockerClient CLIENT = DockerClientBuilder.getInstance().build();

	interface Container
	extends AsyncRunnable  {

		int exitStatusCode();

		String stdOutContent();

		String stdErrContent();
	}
}
