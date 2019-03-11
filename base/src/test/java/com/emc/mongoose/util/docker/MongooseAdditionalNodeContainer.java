package com.emc.mongoose.util.docker;

import static com.emc.mongoose.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.ENTRYPOINT_LIMIT_HEAP_1GB;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.IMAGE_NAME;
import static java.util.Collections.emptyList;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MongooseAdditionalNodeContainer extends ContainerBase {

	public static final int DEFAULT_PORT = 10000;
	public static final int ADDITIONAL_PORT = 9998; // default remote API port is 9999, will conflict with entry node
	private static final Map<String, Path> VOLUME_BINDS = new HashMap<String, Path>() {
		{
			put(CONTAINER_SHARE_PATH, HOST_SHARE_PATH);
		}
	};

	public MongooseAdditionalNodeContainer() throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, DEFAULT_PORT);
	}

	public MongooseAdditionalNodeContainer(final int port) throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, port);
	}

	public MongooseAdditionalNodeContainer(final String version, final int svcPort)
					throws InterruptedException {
		this(version, svcPort, DEFAULT_MEMORY_LIMIT);
	}

	public MongooseAdditionalNodeContainer(
					final String version, final int svcPort, final long memoryLimit) throws InterruptedException {
		super(version, emptyList(), VOLUME_BINDS, false, false, memoryLimit, svcPort);
	}

	@Override
	protected String imageName() {
		return IMAGE_NAME;
	}

	@Override
	protected List<String> containerArgs() {
		return Arrays.asList(
						"--run-node",
						// ports[0] contains the svcPort constructor arg value
						"--load-step-node-port=" + ports[0],
						"--run-port=" + ADDITIONAL_PORT);
	}

	@Override
	protected String entrypoint() {
		return ENTRYPOINT_LIMIT_HEAP_1GB;
	}
}
