package com.emc.mongoose.integ.core.api.s3;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.data.model.ListItemOutput;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 06.08.15.
 */
public class S3ReadZeroSizedItemsFromBucket {
	//
	private final static int COUNT_TO_WRITE = 10000;
	private final static String BUCKET_NAME = S3ReadZeroSizedItemsFromBucket.class.getSimpleName();
	private final static List<WSObject> BUFF_READ = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefaultCfg());
		RunTimeConfig.getContext().set(
			RunTimeConfig.KEY_RUN_ID, S3ReadZeroSizedItemsFromBucket.class.getCanonicalName()
		);
		//
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(BUCKET_NAME)
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, 0);
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(
					null, new ListItemOutput<>(BUFF_READ), COUNT_WRITTEN, 10, true
				);
			} else {
				throw new IllegalStateException("Failed to write");
			}
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final Bucket bucket = new WSBucketImpl(
			(WSRequestConfigImpl) WSLoadBuilderFactory.getInstance(rtConfig).getRequestConfig(),
			BUCKET_NAME, false
		);
		bucket.delete(rtConfig.getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
	//
	@Test
	public void checkReadItemsAreZeroSized() {
		for(final WSObject obj : BUFF_READ) {
			Assert.assertEquals(0, obj.getSize());
		}
	}
}
