package com.emc.mongoose.util.docker;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.emc.mongoose.util.docker.MongooseContainer.CONTAINER_SHARE_PATH;
import static com.emc.mongoose.util.docker.MongooseContainer.HOST_SHARE_PATH;
import static java.util.Collections.emptyList;

public final class MongooseAdditionalNodeContainer
extends ContainerBase {

	private static final String IMAGE_NAME = "emcmongoose/mongoose";
	public static final int DEFAULT_PORT = 10000;
	private static final Map<String, Path> VOLUME_BINDS = new HashMap<String, Path>() {{
		put(CONTAINER_SHARE_PATH, HOST_SHARE_PATH);
	}};

	public MongooseAdditionalNodeContainer()
	throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, DEFAULT_PORT);
	}

	public MongooseAdditionalNodeContainer(final int port)
	throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, port);
	}

	public MongooseAdditionalNodeContainer(final String version, final int svcPort)
	throws InterruptedException {
		super(version, emptyList(), VOLUME_BINDS, false, svcPort);
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
			"--load-step-node-port=" + ports[0]
		);
	}

	@Override
	protected String entrypoint() {
		return null;
	}
}
