package com.emc.mongoose.system.feature.s3;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 06.08.15.
 */
public class S3ReadZeroSizedItemsFromBucket
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 10000;
	private final static String BUCKET_NAME = S3ReadZeroSizedItemsFromBucket.class.getSimpleName();
	private final static List<HttpDataItem> BUFF_READ = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			AppConfig.KEY_RUN_ID, S3ReadZeroSizedItemsFromBucket.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setDstContainer(BUCKET_NAME)
				.build()
		) {
			COUNT_WRITTEN = client.create(null, COUNT_TO_WRITE, 10, 0);
			RunIdFileManager.flushAll();
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setSrcContainer(BUCKET_NAME)
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
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
	//
	@Test
	public void checkReadItemsAreZeroSized() {
		for(final HttpDataItem obj : BUFF_READ) {
			Assert.assertEquals(0, obj.getSize());
		}
	}
}
