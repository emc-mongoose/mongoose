package com.emc.mongoose.integ.core.api.s3;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
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
public final class S3UsePreExistingBucketTest {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN;
	private static Bucket BUCKET;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		RunTimeConfig.resetContext();
		RunTimeConfig.getContext().set(
			RunTimeConfig.KEY_RUN_ID, S3UsePreExistingBucketTest.class.getCanonicalName()
		);
		//
		final WSRequestConfigImpl reqConf = (WSRequestConfigImpl) WSRequestConfigBase
			.newInstanceFor("s3")
			.setProperties(RunTimeConfig.getContext());
		reqConf.setProperties(RunTimeConfig.getContext());
		BUCKET = new WSBucketImpl(
			reqConf, S3UsePreExistingBucketTest.class.getSimpleName(), false
		);
		BUCKET.create("127.0.0.1");
		if(!BUCKET.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the bucket for test");
		}
		//
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setAPI("s3")
					.setS3Bucket(BUCKET.getName())
					.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		BUCKET.delete(RunTimeConfig.getContext().getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
