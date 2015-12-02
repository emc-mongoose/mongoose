package com.emc.mongoose.integ.feature.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class SwiftReadUsingContainerListingTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	private final static String RUN_ID  = SwiftReadUsingContainerListingTest.class.getCanonicalName();
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftContainer(RUN_ID)
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(null, null, COUNT_WRITTEN, 10, true);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
