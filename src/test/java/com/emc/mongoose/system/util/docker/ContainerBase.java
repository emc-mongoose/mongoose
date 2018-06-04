package com.emc.mongoose.system.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ContainerBase
extends AsyncRunnableBase {

	private static final Logger LOG = Logger.getLogger(StorageMockContainer.class.getSimpleName());
	private static final DockerClient DOCKER_CLIENT = DockerClientBuilder.getInstance().build();

	protected String containerId;

	protected ContainerBase()
	throws InterruptedException{
		final String imageName = imageName();
		try {
			DOCKER_CLIENT.inspectImageCmd(imageName).exec();
		} catch(final NotFoundException e) {
			DOCKER_CLIENT
				.pullImageCmd(imageName)
				.exec(new PullImageResultCallback())
				.awaitCompletion();
		}
	}

	protected abstract String imageName();

	protected abstract List<String> containerArgs();

	protected abstract int[] exposedTcpPorts();

	protected abstract String entrypoint();

	protected final void doStart() {
		containerId = createContainer();
		LOG.info("docker start " + containerId + "...");
		DOCKER_CLIENT.startContainerCmd(containerId).exec();
		try {
			TimeUnit.SECONDS.sleep(30);
		} catch(final InterruptedException e) {
			throw new CancellationException(e.getMessage());
		}
	}

	private String createContainer() {
		final String imageName = imageName();
		final List<String> args = containerArgs();
		final ExposedPort[] exposedPorts = Arrays.stream(exposedTcpPorts())
			.mapToObj(ExposedPort::tcp)
			.collect(Collectors.toList())
			.toArray(new ExposedPort[]{});
		final String entrypoint = entrypoint();
		final CreateContainerResponse container = DOCKER_CLIENT
			.createContainerCmd(imageName)
			.withName(imageName + '_' + hashCode())
			.withNetworkMode("host")
			.withExposedPorts(exposedPorts)
			.withEntrypoint(entrypoint)
			.withCmd(args)
			.exec();
		return container.getId();
	}

	protected final void doClose() {
		LOG.info("docker kill " + containerId + "...");
		DOCKER_CLIENT.killContainerCmd(containerId).exec();
		LOG.info("docker rm " + containerId + "...");
		DOCKER_CLIENT.removeContainerCmd(containerId).exec();
	}
}
