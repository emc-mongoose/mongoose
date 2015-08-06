package com.emc.mongoose.integ.core.api.swift;

import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 06.08.15.
 */
public class SwiftReadRandomSizedItemsFromContainer {
	//
	private final static long COUNT_TO_WRITE = 10000;
	private final static String
		CONTAINER_NAME = SwiftReadUsingContainerListingTest.class.getSimpleName();
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		//
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setS3Bucket(CONTAINER_NAME)
				.build()
		) {
			COUNT_WRITTEN = client.write(
				null, null, COUNT_TO_WRITE, 10, 0, 123456, 3
			);
			COUNT_READ = client.read(null, null, COUNT_TO_WRITE, 10, true);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
		throws Exception {
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
