package com.emc.mongoose.integ.feature.s3;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.adapter.s3.BucketHelper;
import com.emc.mongoose.storage.adapter.s3.HttpBucketHelper;
import com.emc.mongoose.storage.adapter.s3.HttpRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public final class S3UsePreExistingBucketTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN;
	private static BucketHelper bucketHelper;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, S3UsePreExistingBucketTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		final HttpRequestConfigImpl reqConf = (HttpRequestConfigImpl) HttpRequestConfigBase
			.newInstanceFor("s3")
			.setAppConfig(BasicConfig.THREAD_CONTEXT.get());
		reqConf.setAppConfig(BasicConfig.THREAD_CONTEXT.get());
		bucketHelper = new HttpBucketHelper(
			reqConf, new BasicContainer(S3UsePreExistingBucketTest.class.getSimpleName())
		);
		bucketHelper.create("127.0.0.1");
		if(!bucketHelper.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the bucket for test");
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(bucketHelper.toString())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		bucketHelper.delete(BasicConfig.THREAD_CONTEXT.get().getStorageAddrs()[0]);
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
