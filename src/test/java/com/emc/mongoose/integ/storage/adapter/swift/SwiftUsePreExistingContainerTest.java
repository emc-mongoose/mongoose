package com.emc.mongoose.integ.storage.adapter.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.tools.LogParser;
import com.emc.mongoose.storage.adapter.swift.Container;
import com.emc.mongoose.storage.adapter.swift.WSContainerImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class SwiftUsePreExistingContainerTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	private final static String RUN_ID = SwiftUsePreExistingContainerTest.class.getCanonicalName();
	//
	private static long COUNT_WRITTEN;
	private static Container<WSObject> CONTAINER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		StandaloneClientTestBase.setUpClass();
		//
		final WSRequestConfigImpl reqConf = (WSRequestConfigImpl) WSRequestConfigBase
			.newInstanceFor("swift")
			.setProperties(RunTimeConfig.getContext());
		CONTAINER = new WSContainerImpl<>(reqConf, RUN_ID, false);
		CONTAINER.create("127.0.0.1");
		if(!CONTAINER.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the bucket for test");
		}
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftContainer(CONTAINER.getName())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			//
			LogParser.flushAllLogs();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CONTAINER.delete(RunTimeConfig.getContext().getStorageAddrs()[0]);
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
