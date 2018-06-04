package com.emc.mongoose.system.util.docker;

import java.util.List;

public class MongooseNodeSvcContainer
extends ContainerBase {

	@Override
	protected String imageName() {
		return null;
	}

	@Override
	protected List<String> containerArgs() {
		return null;
	}

	@Override
	protected int[] exposedTcpPorts() {
		return new int[0];
	}

	@Override
	protected String entrypoint() {
		return null;
	}
}
