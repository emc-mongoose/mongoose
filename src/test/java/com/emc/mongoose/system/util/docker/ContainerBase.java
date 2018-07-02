package com.emc.mongoose.system.util.docker;

import com.github.akurilov.commons.concurrent.AsyncRunnableBase;

import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.DockerClientException;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.command.PullImageResultCallback;
import com.github.dockerjava.core.command.WaitContainerResultCallback;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.emc.mongoose.system.util.docker.Docker.DEFAULT_IMAGE_VERSION;

public abstract class ContainerBase
extends AsyncRunnableBase
implements Docker.Container {

	private static final Logger LOG = Logger.getLogger(HttpStorageMockContainer.class.getSimpleName());

	private final String version;
	private final List<String> env;
	private final Map<String, Path> volumeBinds;
	private final boolean attachOutputFlag;
	protected final int[] exposedTcpPorts;
	private final StringBuilder stdOutBuff = new StringBuilder();
	private final StringBuilder stdErrBuff = new StringBuilder();
	private final ResultCallback<Frame> streamsCallback = new ContainerOutputCallback(
		stdOutBuff, stdErrBuff
	);
	private String containerId;
	private int containerExitCode = Integer.MIN_VALUE;

	protected ContainerBase(
		final String version, final List<String> env, final Map<String, Path> volumeBinds,
		final boolean attachOutputFlag, final int... exposedTcpPorts
	) throws InterruptedException{
		this.version = (version == null || version.isEmpty()) ? DEFAULT_IMAGE_VERSION : version;
		this.env = env;
		this.volumeBinds = volumeBinds;
		this.attachOutputFlag = attachOutputFlag;
		this.exposedTcpPorts = exposedTcpPorts;
		final String imageNameWithVer = imageName() + ":" + this.version;
		try {
			Docker.CLIENT.inspectImageCmd(imageNameWithVer).exec();
		} catch(final NotFoundException e) {
			Docker.CLIENT
				.pullImageCmd(imageNameWithVer)
				.exec(new PullImageResultCallback())
				.awaitCompletion();
		}
	}

	protected abstract String imageName();

	protected abstract List<String> containerArgs();

	protected abstract String entrypoint();

	@Override
	public final int exitStatusCode() {
		return containerExitCode;
	}

	@Override
	public final String stdOutContent() {
		return stdOutBuff.toString();
	}

	@Override
	public final String stdErrContent() {
		return stdErrBuff.toString();
	}

	protected final void doStart() {
		containerId = createContainer();
		if(attachOutputFlag) {
			Docker.CLIENT
				.attachContainerCmd(containerId)
				.withStdErr(true)
				.withStdOut(true)
				.withFollowStream(true)
				.exec(streamsCallback);
		}
		LOG.info("docker start " + imageName() + "(" + containerId + ")...");
		Docker.CLIENT.startContainerCmd(containerId).exec();
	}

	private String createContainer() {
		final String imageNameWithVer = imageName() + ":" + this.version;
		final List<String> args = containerArgs();
		final ExposedPort[] exposedPorts = Arrays.stream(exposedTcpPorts)
			.mapToObj(ExposedPort::tcp)
			.collect(Collectors.toList())
			.toArray(new ExposedPort[]{});
		final CreateContainerCmd createContainerCmd = Docker.CLIENT
			.createContainerCmd(imageNameWithVer)
			.withName(imageName().replace('/', '_') + '_' + hashCode())
			.withNetworkMode("host")
			.withExposedPorts(exposedPorts)
			.withCmd(args);
		if(env != null && !env.isEmpty()) {
			createContainerCmd.withEnv(env);
		}
		if(attachOutputFlag) {
			createContainerCmd
				.withAttachStdout(attachOutputFlag)
				.withAttachStderr(attachOutputFlag);
		}
		if(volumeBinds != null && !volumeBinds.isEmpty()) {
			final List<Volume> volumes = new ArrayList<>(volumeBinds.size());
			final List<Bind> binds = new ArrayList<>(volumeBinds.size());
			volumeBinds.forEach(
				(containerPath, hostPath) -> {
					final Volume volume = new Volume(containerPath);
					volumes.add(volume);
					final Bind bind = new Bind(hostPath.toString(), volume);
					binds.add(bind);
				}
			);
			createContainerCmd.withVolumes(volumes);
			createContainerCmd.withBinds(binds);
		}
		final String entrypoint = entrypoint();
		if(entrypoint != null && !entrypoint.isEmpty()) {
			createContainerCmd.withEntrypoint(entrypoint);
		}
		final CreateContainerResponse container = createContainerCmd.exec();
		return container.getId();
	}

	public final boolean await(final long timeout, final TimeUnit timeUnit) {
		try {
			containerExitCode = Docker.CLIENT
				.waitContainerCmd(containerId)
				.exec(new WaitContainerResultCallback())
				.awaitStatusCode(timeout, TimeUnit.SECONDS);
			return true;
		} catch(final DockerClientException e) {
			System.err.println(e.getMessage());
			return false;
		}
	}

	protected final void doClose() {

		LOG.info("docker kill " + containerId + "...");
		try {
			Docker.CLIENT.killContainerCmd(containerId).exec();
		} catch(final Throwable t) {
			t.printStackTrace(System.err);
		}

		LOG.info("docker rm " + containerId + "...");
		try {
			Docker.CLIENT.removeContainerCmd(containerId).exec();
		} catch(final Throwable t) {
			t.printStackTrace(System.err);
		}

		try {
			streamsCallback.close();
		} catch(final IOException e) {
			e.printStackTrace(System.err);
		}
	}
}
