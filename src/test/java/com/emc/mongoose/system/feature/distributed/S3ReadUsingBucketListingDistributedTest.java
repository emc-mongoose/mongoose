package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
//
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.system.base.DistributedClientTestBase;
//
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public final class S3ReadUsingBucketListingDistributedTest
extends DistributedClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	private final static String RUN_ID = S3ReadUsingBucketListingDistributedTest.class.getCanonicalName();
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		DistributedClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setDstContainer(RUN_ID)
				.build()
		) {
			COUNT_WRITTEN = client.create(null, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("10KB"));
			RunIdFileManager.flushAll();
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setSrcContainer(RUN_ID)
				.build();
		) {
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(null, null, COUNT_WRITTEN, 10, true);
			} else {
				throw new IllegalStateException("Failed to write");
			}
		}
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE, COUNT_TO_WRITE / 1000);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ, COUNT_TO_WRITE / 1000);
	}
}
