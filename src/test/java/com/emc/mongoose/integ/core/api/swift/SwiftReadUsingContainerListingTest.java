package com.emc.mongoose.integ.core.api.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.storage.adapter.swift.Container;
import com.emc.mongoose.storage.adapter.swift.WSContainerImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import com.emc.mongoose.util.scenario.shared.WSLoadBuilderFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class SwiftReadUsingContainerListingTest {
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
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			COUNT_READ = client.read(null, null, COUNT_TO_WRITE, 10, true);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final Container container = new WSContainerImpl(
			(WSRequestConfigImpl) WSLoadBuilderFactory.getInstance(rtConfig).getRequestConfig(),
			CONTAINER_NAME, false
		);
		container.delete(rtConfig.getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
