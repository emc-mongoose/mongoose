package com.emc.mongoose.util.docker;

import static com.emc.mongoose.util.docker.Docker.DEFAULT_IMAGE_VERSION;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

import java.util.ArrayList;
import java.util.List;

public final class HttpStorageMockContainer extends ContainerBase {

	public static final int DEFAULT_PORT = 8000;
	public static final int DEFAULT_CAPACITY = 1_000_000;
	public static final int DEFAULT_CONTAINER_CAPACITY = 1_000_000;
	public static final int DEFAULT_CONTAINER_COUNT_LIMIT = 1_000_000;
	public static final int DEFAULT_FAIL_CONNECT_EVERY = 0;
	public static final int DEFAULT_FAIL_RESPONSES_EVERY = 0;

	private static final String IMAGE_NAME = "emcmongoose/nagaina";

	private final String itemInputFile;
	private final String itemNamingPrefix;
	private final int itemNamingRadix;
	private final int capacity;
	private final int containerCapacity;
	private final int containerCountLimit;
	private final int failConnectEvery;
	private final int failResponsesEvery;
	private final boolean sslFlag;
	private final double rateLimit;

	public HttpStorageMockContainer(
					final int port,
					final boolean sslFlag,
					final String itemInputFile,
					final String itemNamingPrefix,
					final int itemNamingRadix,
					final int capacity,
					final int containerCountLimit,
					final int containerCapacity,
					final int failConnectEvery,
					final int failResponsesEvery,
					final double rateLimit)
					throws Exception {
		super(DEFAULT_IMAGE_VERSION, emptyList(), emptyMap(), false, false, port);
		this.itemInputFile = itemInputFile;
		this.itemNamingPrefix = itemNamingPrefix;
		this.itemNamingRadix = itemNamingRadix;
		this.capacity = capacity;
		this.containerCapacity = containerCapacity;
		this.containerCountLimit = containerCountLimit;
		this.failConnectEvery = failConnectEvery;
		this.failResponsesEvery = failResponsesEvery;
		this.sslFlag = sslFlag;
		this.rateLimit = rateLimit;
	}

	@Override
	protected final String imageName() {
		return IMAGE_NAME;
	}

	@Override
	protected List<String> containerArgs() {
		final List<String> cmd = new ArrayList<>();
		cmd.add("-jar");
		cmd.add("/opt/nagaina/nagaina.jar");
		if (itemInputFile != null && !itemInputFile.isEmpty()) {
			cmd.add("--item-input-file=" + itemInputFile);
		}
		if (itemNamingPrefix != null) {
			cmd.add("--item-naming-prefix=" + itemNamingPrefix);
		}
		cmd.add("--item-naming-radix=" + itemNamingRadix);
		cmd.add("--storage-mock-capacity=" + capacity);
		cmd.add("--storage-mock-container-capacity=" + containerCapacity);
		cmd.add("--storage-mock-container-countLimit=" + containerCountLimit);
		cmd.add("--storage-mock-fail-connections=" + failConnectEvery);
		cmd.add("--storage-mock-fail-responses=" + failResponsesEvery);
		cmd.add("--storage-net-node-port=" + ports[0]);
		cmd.add("--storage-net-ssl=" + sslFlag);
		cmd.add("--test-step-limit-rate=" + rateLimit);
		return cmd;
	}

	@Override
	protected String entrypoint() {
		return "java";
	}
}
