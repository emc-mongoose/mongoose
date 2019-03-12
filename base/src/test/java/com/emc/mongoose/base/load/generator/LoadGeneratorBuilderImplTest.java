package com.emc.mongoose.base.load.generator;

import static com.emc.mongoose.base.Constants.APP_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.emc.mongoose.base.env.Extension;
import com.emc.mongoose.base.item.DataItem;
import com.emc.mongoose.base.item.DataItemFactoryImpl;
import com.emc.mongoose.base.item.ItemFactory;
import com.emc.mongoose.base.item.ItemType;
import com.emc.mongoose.base.item.op.OpType;
import com.emc.mongoose.base.item.op.Operation;
import com.emc.mongoose.base.item.op.data.DataOperation;
import com.emc.mongoose.base.storage.Credential;
import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.commons.io.collection.IoBuffer;
import com.github.akurilov.commons.io.collection.LimitedQueueBuffer;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.impl.BasicConfig;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

public class LoadGeneratorBuilderImplTest {

	private static final Map<String, Object> CONFIG_SCHEMA;

	static {
		try {
			final var configSchemas = Extension.load(Thread.currentThread().getContextClassLoader()).stream()
							.map(Extension::schemaProvider)
							.filter(Objects::nonNull)
							.map(
											schemaProvider -> {
												try {
													return schemaProvider.schema();
												} catch (final Exception e) {
													fail(e.getMessage());
												}
												return null;
											})
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
			SchemaProvider.resolve(APP_NAME, Thread.currentThread().getContextClassLoader()).stream()
							.findFirst()
							.ifPresent(configSchemas::add);
			CONFIG_SCHEMA = TreeUtil.reduceForest(configSchemas);
		} catch (final Throwable cause) {
			throw new AssertionError(cause);
		}
	}

	@Test
	public void multiBucketPerUserTest() throws Exception {

		final var credentialsFilePath = Files.createTempFile(getClass().getSimpleName(), ".csv");
		credentialsFilePath.toFile().deleteOnExit();
		final var bucketCount = 100;
		final var opCount = 10000;
		final var prefixUid = "user-";
		final var prefixSecret = "secret-";
		final var prefixBucket = "bucket-";
		try (final var bw = Files.newBufferedWriter(credentialsFilePath)) {
			for (var i = 0; i < bucketCount; i++) {
				bw.append(prefixBucket)
								.append(Integer.toString(i))
								.append(',')
								.append(prefixUid)
								.append(Integer.toString(i))
								.append(',')
								.append(prefixSecret)
								.append(Integer.toString(i));
				bw.newLine();
			}
		}
		final var seed = 314159265;
		final Map<String, Object> options = new HashMap<>() {
			{
				put("item-data-ranges-concat", null);
				put("item-data-ranges-fixed", null);
				put("item-data-ranges-random", 0);
				put("item-data-ranges-threshold", 0);
				put("item-data-size", "1MB");
				put("item-input-path", null);
				put("item-naming-length", 12);
				put("item-naming-seed", 0L);
				put("item-naming-prefix", null);
				put("item-naming-radix", 36);
				put("item-naming-step", 1);
				put("item-naming-type", "random");
				put("item-output-path", prefixBucket + "${rnd.nextLong(100)}%{" + seed + "}");
				put("load-batch-size", opCount);
				put("load-op-limit-count", opCount);
				put("load-op-limit-recycle", 1_000_000);
				put("load-op-recycle", false);
				put("load-op-retry", false);
				put("load-op-shuffle", false);
				put("load-op-type", OpType.CREATE.name().toLowerCase());
				put("storage-auth-file", credentialsFilePath.toAbsolutePath().toString());
			}
		};
		final var config = (Config) new BasicConfig("-", CONFIG_SCHEMA);
		options.forEach(config::val);
		final var itemFactory = (ItemFactory) new DataItemFactoryImpl();
		final List<DataOperation<DataItem>> ops = new ArrayList<>(opCount);

		try (final IoBuffer<DataOperation<DataItem>> opBuff = new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(opCount));
						final LoadGenerator loadGenerator = new LoadGeneratorBuilderImpl()
										.authConfig(config.configVal("storage-auth"))
										.itemConfig(config.configVal("item"))
										.itemFactory(itemFactory)
										.itemType(ItemType.DATA)
										.loadConfig(config.configVal("load"))
										.loadOperationsOutput(opBuff)
										.originIndex(0)
										.build()) {
			loadGenerator.start();
			if (!loadGenerator.await(10, TimeUnit.SECONDS)) {
				throw new AssertionError("Load generator await timeout");
			}
			assertEquals(opCount, loadGenerator.generatedOpCount());
			assertEquals(opCount, opBuff.size());
			assertEquals(opCount, opBuff.get(ops, opCount));
		}

		String bucket;
		Credential credential;
		String uid;
		String secret;
		String suffix;
		long n;
		final var freq = new Frequency();
		for (final Operation op : ops) {
			bucket = op.dstPath();
			suffix = bucket.substring(prefixBucket.length());
			n = Long.parseLong(suffix);
			assertTrue(n >= 0);
			assertTrue(n < bucketCount);
			freq.addValue(n);
			credential = op.credential();
			uid = credential.getUid();
			assertEquals(prefixUid + suffix, uid);
			secret = credential.getSecret();
			assertEquals(prefixSecret + suffix, secret);
		}
		ops.clear();
		final var expectedFreq = opCount / bucketCount;
		for (var i = 0; i < bucketCount; i++) {
			assertEquals(expectedFreq, freq.getCount(i), expectedFreq / 3);
		}
	}
}
