package com.emc.mongoose.integ.feature.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.adapter.swift.SwiftContainerHelper;
import com.emc.mongoose.storage.adapter.swift.HttpSwiftContainerHelper;
import com.emc.mongoose.storage.adapter.swift.HttpRequestConfigImpl;
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
	private static SwiftContainerHelper<HttpDataItem, Container<HttpDataItem>> CONTAINER_HELPER;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		StandaloneClientTestBase.setUpClass();
		//
		final HttpRequestConfigImpl reqConf = (HttpRequestConfigImpl) HttpRequestConfigBase
			.newInstanceFor("swift")
			.setAppConfig(BasicConfig.CONTEXT_CONFIG.get());
		CONTAINER_HELPER = new HttpSwiftContainerHelper<HttpDataItem, Container<HttpDataItem>>(
				reqConf, new BasicContainer(RUN_ID)
		);
		CONTAINER_HELPER.create("127.0.0.1");
		if(!CONTAINER_HELPER.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the bucket for test");
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftContainer(CONTAINER_HELPER.toString())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CONTAINER_HELPER.delete(BasicConfig.CONTEXT_CONFIG.get().getStorageAddrs()[0]);
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
