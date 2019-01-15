package com.emc.mongoose.load.generator;

import com.emc.mongoose.env.Extension;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.DataItemFactoryImpl;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemNamingType;
import com.emc.mongoose.item.ItemType;
import com.emc.mongoose.item.op.OpType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.item.op.data.DataOperation;
import static com.emc.mongoose.Constants.APP_NAME;

import com.emc.mongoose.storage.Credential;
import com.github.akurilov.commons.collection.TreeUtil;
import com.github.akurilov.commons.io.collection.IoBuffer;
import com.github.akurilov.commons.io.collection.LimitedQueueBuffer;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.SchemaProvider;
import com.github.akurilov.confuse.impl.BasicConfig;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.commons.math3.stat.Frequency;
import org.junit.Test;

public class LoadGeneratorBuilderImplTest {

	private static final Map<String, Object> CONFIG_SCHEMA;
	static {
		try {
			final List<Map<String, Object>> configSchemas = Extension
				.load(Thread.currentThread().getContextClassLoader())
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
				.resolve(APP_NAME, Thread.currentThread().getContextClassLoader())
				.stream()
				.findFirst()
				.ifPresent(configSchemas::add);
			CONFIG_SCHEMA = TreeUtil.reduceForest(configSchemas);
		} catch(final Throwable cause) {
			throw new AssertionError(cause);
		}
	}

	@Test
	public void multiBucketPerUserTest()
	throws Exception {

		final Path credentialsFilePath = Files.createTempFile(getClass().getSimpleName(), ".csv");
		credentialsFilePath.toFile().deleteOnExit();
		final int bucketCount = 100;
		final int opCount = 10000;
		final String prefixUid = "user-";
		final String prefixSecret = "secret-";
		final String prefixBucket = "bucket-";
		try(final BufferedWriter bw = Files.newBufferedWriter(credentialsFilePath)) {
			for(int i = 0; i < bucketCount; i ++) {
				bw
					.append(prefixUid)
					.append(i < 10 ? "0" : "")
					.append(Integer.toString(i))
					.append(',')
					.append(prefixSecret)
					.append(i < 10 ? "0" : "")
					.append(Integer.toString(i));
				bw.newLine();
			}
		}
		final int seed = 314159265;
		final Map<String, Object> options = new HashMap<String, Object>() {{
			put("item-data-ranges-concat", null);
			put("item-data-ranges-fixed", null);
			put("item-data-ranges-random", 0);
			put("item-data-ranges-threshold", 0);
			put("item-data-size", "1MB");
			put("item-input-path", null);
			put("item-naming-length", 13);
			put("item-naming-offset", 0);
			put("item-naming-prefix", null);
			put("item-naming-radix", 36);
			put("item-naming-type", ItemNamingType.RANDOM.name().toLowerCase());
			put("item-output-path", prefixBucket + "%d(" + seed + "){00}[0-99]");
			put("load-batch-size", opCount);
			put("load-op-limit-count", opCount);
			put("load-op-limit-recycle", 1_000_000);
			put("load-op-recycle", false);
			put("load-op-retry", false);
			put("load-op-shuffle", false);
			put("load-op-type", OpType.CREATE.name().toLowerCase());
			put("storage-auth-file", credentialsFilePath.toAbsolutePath().toString());
			put("storage-auth-uid", prefixUid + "%d(" + seed + "){00}[0-99]");
		}};
		final Config config = new BasicConfig("-", CONFIG_SCHEMA);
		options.forEach(config::val);
		final ItemFactory itemFactory = new DataItemFactoryImpl();
		final List<DataOperation<DataItem>> ops = new ArrayList<>(opCount);

		try(
			final IoBuffer<DataOperation<DataItem>> opBuff =
				new LimitedQueueBuffer<>(new ArrayBlockingQueue<>(opCount));
			final LoadGenerator loadGenerator = new LoadGeneratorBuilderImpl()
				.authConfig(config.configVal("storage-auth"))
				.itemConfig(config.configVal("item"))
				.itemFactory(itemFactory)
				.itemType(ItemType.DATA)
				.loadConfig(config.configVal("load"))
				.loadOperationsOutput(opBuff)
				.originIndex(0)
				.build()
		) {
			loadGenerator.start();
			if(!loadGenerator.await(10, TimeUnit.SECONDS)) {
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
		int n;
		final Frequency freq = new Frequency();
		for(final Operation op: ops) {
			bucket = op.dstPath();
			suffix = bucket.substring(prefixBucket.length());
			n = Integer.parseInt(suffix);
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
		final int expectedFreq = opCount / bucketCount;
		for(int i = 0; i < bucketCount; i ++) {
			assertEquals(expectedFreq, freq.getCount(i), expectedFreq / 3);
		}
	}
}
