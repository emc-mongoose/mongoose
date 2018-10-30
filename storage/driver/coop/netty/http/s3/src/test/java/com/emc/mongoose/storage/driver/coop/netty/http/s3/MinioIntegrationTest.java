package com.emc.mongoose.storage.driver.coop.netty.http.s3;

import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.DataItemImpl;
import com.emc.mongoose.item.op.data.DataOperation;
import com.emc.mongoose.item.op.data.DataOperationImpl;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.storage.driver.StorageDriver;
import com.emc.mongoose.storage.driver.coop.netty.http.s3.util.MinioContainer;
import static com.emc.mongoose.Constants.APP_NAME;
import static com.emc.mongoose.item.op.OpType.CREATE;
import static com.emc.mongoose.item.op.Operation.Status.SUCC;

import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.commons.concurrent.AsyncRunnable;
import com.github.akurilov.commons.system.SizeInBytes;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.impl.BasicConfig;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;
import java.util.stream.Collectors;
import static java.lang.Thread.currentThread;
import static java.util.Collections.EMPTY_MAP;
import static java.util.Collections.singletonList;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MinioIntegrationTest {

	private static final Credential CREDENTIAL = Credential.getInstance(
		"AKIAIOSFODNN7EXAMPLE", "wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY"
	);

	private static DataInput dataInput;
	private static AsyncRunnable storageContainer;
	private static StorageDriver<DataItem, DataOperation<DataItem>> storageDriver;

	@BeforeClass
	public static void setUpClass()
	throws Exception {
		final List<Map<String, Object>> configSchemas = Extension
			.load(currentThread().getContextClassLoader())
			.stream()
			.map(Extension::schemaProvider)
			.filter(Objects::nonNull)
			.map(
				schemaProvider -> {
					try {
						return schemaProvider.schema();
					} catch(final Exception e) {
						fail(e.getMessage());
					}
					return null;
				}
			)
			.filter(Objects::nonNull)
			.collect(Collectors.toList());
		SchemaProvider
			.resolve(APP_NAME, currentThread().getContextClassLoader())
			.stream()
			.findFirst()
			.ifPresent(configSchemas::add);
		final Map<String, Object> configSchema = TreeUtil.reduceForest(configSchemas);
		final Config config = new BasicConfig("-", configSchema);
		config.val("storage-driver-limit-concurrency", 0);
		config.val("storage-net-transport", "epoll");
		config.val("storage-net-reuseAddr", true);
		config.val("storage-net-bindBacklogSize", 0);
		config.val("storage-net-keepAlive", true);
		config.val("storage-net-rcvBuf", 0);
		config.val("storage-net-sndBuf", 0);
		config.val("storage-net-ssl", false);
		config.val("storage-net-tcpNoDelay", false);
		config.val("storage-net-interestOpQueued", false);
		config.val("storage-net-linger", 0);
		config.val("storage-net-timeoutMilliSec", 0);
		config.val("storage-net-ioRatio", 50);
		config.val("storage-net-node-addrs", singletonList("127.0.0.1"));
		config.val("storage-net-node-port", MinioContainer.PORT);
		config.val("storage-net-node-connAttemptsLimit", 0);
		config.val("storage-net-http-fsAccess", true);
		config.val("storage-net-http-versioning", true);
		config.val("storage-net-http-headers", EMPTY_MAP);
		config.val("storage-net-http-uri-args", EMPTY_MAP);
		config.val("storage-auth-uid", CREDENTIAL.getUid());
		config.val("storage-auth-token", null);
		config.val("storage-auth-secret", CREDENTIAL.getSecret());
		config.val("storage-driver-limit-queue-input", 1_000_000);
		config.val("storage-driver-limit-queue-output", 1_000_000);
		config.val("storage-driver-type", "s3");
		config.val("storage-driver-threads", 0);
		storageContainer = new MinioContainer("latest", CREDENTIAL.getUid(), CREDENTIAL.getSecret(), Paths.get("/tmp"));
		storageContainer.start();
		dataInput = DataInput.instance(null, "7a42d9c483244167", new SizeInBytes("4MB"), 16);
		storageDriver = new AmzS3StorageDriver<>(
			"test-storage-driver-s3", dataInput, config.configVal("storage"), false, 4096
		);
		storageDriver.start();
	}

	@AfterClass
	public static void tearDownClass()
	throws Exception {
		storageDriver.close();
		storageContainer.close();
	}

	@Test
	public final void testCreateObject()
	throws Exception {
		final DataItem object = new DataItemImpl(0, 10240);
		object.dataInput(dataInput);
		final DataOperation<DataItem> createObjOp = new DataOperationImpl<>(
			0, CREATE, object, null, "/play/test_object", CREDENTIAL, null, 0
		);
		assertTrue(storageDriver.put(createObjOp));
		while(!storageDriver.hasRemainingResults()) {
			LockSupport.parkNanos(1);
		}
		final DataOperation<DataItem> createObjOpResult = storageDriver.get();
		assertNotNull(createObjOpResult);
		assertEquals(SUCC, createObjOpResult.status());
	}
}
