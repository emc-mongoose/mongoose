package com.emc.mongoose.system.util.docker;

import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public final class MongooseSlaveNodeContainer
extends ContainerBase {

	private static final String IMAGE_NAME = "emcmongoose/mongoose";
	public static final int DEFAULT_PORT = 10000;

	public MongooseSlaveNodeContainer()
	throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, DEFAULT_PORT);
	}

	public MongooseSlaveNodeContainer(final int port)
	throws InterruptedException {
		this(MongooseContainer.IMAGE_VERSION, port);
	}

	public MongooseSlaveNodeContainer(final String version, final int svcPort)
	throws InterruptedException {
		super(version, emptyList(), emptyMap(), false, svcPort);
	}

	@Override
	protected String imageName() {
		return IMAGE_NAME;
	}

	@Override
	protected List<String> containerArgs() {
		return Arrays.asList(
			"--run-node",
			// exposedTcpPorts[0] contains the svcPort constructor arg value
			"--load-step-node-port=" + exposedTcpPorts[0]
		);
	}

	@Override
	protected String entrypoint() {
		return null;
	}
}
