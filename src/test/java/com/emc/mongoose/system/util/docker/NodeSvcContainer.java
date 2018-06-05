package com.emc.mongoose.system.util.docker;

import java.util.Arrays;
import java.util.List;

public final class NodeSvcContainer
extends ContainerBase {

	private static final String IMAGE_NAME = "emcmongoose/mongoose";

	public NodeSvcContainer(final String version, final int svcPort)
	throws InterruptedException {
		super(version, svcPort);
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
