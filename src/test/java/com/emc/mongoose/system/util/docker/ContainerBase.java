package com.emc.mongoose.system.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.PullImageResultCallback;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public abstract class ContainerBase
extends AsyncRunnableBase {

	protected static final String VERSION_DEFAULT = "latest";

	private static final Logger LOG = Logger.getLogger(StorageMockContainer.class.getSimpleName());
	private static final DockerClient DOCKER_CLIENT = DockerClientBuilder.getInstance().build();

	protected final String version;
	protected final int[] exposedTcpPorts;

	protected String containerId;

	protected ContainerBase(final String version, final int... exposedTcpPorts)
	throws InterruptedException{
		this.version = (version == null || version.isEmpty()) ? VERSION_DEFAULT : version;
		this.exposedTcpPorts = exposedTcpPorts;
		final String imageNameWithVer = imageName() + ":" + this.version;
		try {
			DOCKER_CLIENT.inspectImageCmd(imageNameWithVer).exec();
		} catch(final NotFoundException e) {
			DOCKER_CLIENT
				.pullImageCmd(imageNameWithVer)
				.exec(new PullImageResultCallback())
				.awaitCompletion();
		}
	}

	protected abstract String imageName();

	protected abstract List<String> containerArgs();

	protected abstract String entrypoint();

	protected final void doStart() {
		containerId = createContainer();
		LOG.info("docker start " + imageName() + "(" + containerId + ")...");
		DOCKER_CLIENT.startContainerCmd(containerId).exec();
	}

	private String createContainer() {
		final String imageNameWithVer = imageName() + ":" + this.version;
		final List<String> args = containerArgs();
		final ExposedPort[] exposedPorts = Arrays.stream(exposedTcpPorts)
			.mapToObj(ExposedPort::tcp)
			.collect(Collectors.toList())
			.toArray(new ExposedPort[]{});
		final CreateContainerCmd createContainerCmd = DOCKER_CLIENT
			.createContainerCmd(imageNameWithVer)
			.withName(imageName().replace('/', '_') + '_' + hashCode())
			.withNetworkMode("host")
			.withExposedPorts(exposedPorts)
			.withCmd(args);
		final String entrypoint = entrypoint();
		if(entrypoint != null && !entrypoint.isEmpty()) {
			createContainerCmd.withEntrypoint(entrypoint);
		}
		final CreateContainerResponse container = createContainerCmd.exec();
		return container.getId();
	}

	protected final void doClose() {
		LOG.info("docker kill " + containerId + "...");
		DOCKER_CLIENT.killContainerCmd(containerId).exec();
		LOG.info("docker rm " + containerId + "...");
		DOCKER_CLIENT.removeContainerCmd(containerId).exec();
	}
}
