package com.emc.mongoose.storage.driver.coop.netty.http.s3.util;

import com.emc.mongoose.util.docker.ContainerBase;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static java.util.Arrays.asList;

public class MinioContainer
extends ContainerBase {

	public static final int PORT = 9000;

	private static final String IMAGE_NAME = "minio/minio";
	private static final String CONTAINER_DATA_PATH = "/data";
	private static final String ARG = "server";
	private static final String MINIO_ACCESS_KEY = "MINIO_ACCESS_KEY";
	private static final String MINIO_SECRET_KEY = "MINIO_SECRET_KEY";

	private MinioContainer(
		final String version, final List<String> env, final Map<String, Path> volumeBinds,
		final boolean attachOutputFlag, final boolean collectOutputFlag, final int... ports
	) throws InterruptedException {
		super(version, env, volumeBinds, attachOutputFlag, collectOutputFlag, ports);
	}

	public MinioContainer(
		final String version, final String accessKey, final String secretKey, final Path hostDataPath
	) throws InterruptedException {
		this(
			version, asList(MINIO_ACCESS_KEY + "=" + accessKey, MINIO_SECRET_KEY + "=" + secretKey),
			new HashMap<String, Path>() {{ put(CONTAINER_DATA_PATH, hostDataPath); }}, true, false, PORT
		);
	}

	@Override
	protected final String imageName() {
		return IMAGE_NAME;
	}

	@Override
	protected final List<String> containerArgs() {
		return asList(ARG, CONTAINER_DATA_PATH);
	}

	@Override
	protected final String entrypoint() {
		return null;
	}
}
