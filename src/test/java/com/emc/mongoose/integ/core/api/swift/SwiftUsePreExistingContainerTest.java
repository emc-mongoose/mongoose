package com.emc.mongoose.integ.core.api.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.storage.adapter.swift.Container;
import com.emc.mongoose.storage.adapter.swift.WSContainerImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class SwiftUsePreExistingContainerTest {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN;
	private static Container CONTAINER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		RunTimeConfig.setContext(RunTimeConfig.getDefaultCfg());
		RunTimeConfig.getContext().set(
			RunTimeConfig.KEY_RUN_ID, SwiftUsePreExistingContainerTest.class.getCanonicalName()
		);
		//
		final WSRequestConfigImpl reqConf = new WSRequestConfigImpl();
		reqConf.setProperties(RunTimeConfig.getContext());
		CONTAINER = new WSContainerImpl(
			reqConf, SwiftUsePreExistingContainerTest.class.getSimpleName(), false
		);
		CONTAINER.create("127.0.0.1");
		if(!CONTAINER.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the bucket for test");
		}
		//
		try(
			final StorageClient<WSObject> client = new BasicWSClientBuilder<>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftContainer(CONTAINER.getName())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CONTAINER.delete("127.0.0.1");
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
