package com.emc.mongoose.integ.feature.atmos;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.io.conf.HttpRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.adapter.atmos.HttpRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.AtmosSubTenantHelper;
import com.emc.mongoose.storage.mock.impl.http.request.AtmosRequestHandler;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosUsePreExistingSubtenantTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN;
	private static AtmosSubTenantHelper SUBTENANT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			AppConfig.KEY_RUN_ID, AtmosUsePreExistingSubtenantTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		final AppConfig appConfig = BasicConfig.THREAD_CONTEXT.get();
		final HttpRequestConfigImpl reqConf = (HttpRequestConfigImpl) HttpRequestConfigBase
			.newInstanceFor("atmos")
			.setAppConfig(appConfig);
		reqConf.setAppConfig(BasicConfig.THREAD_CONTEXT.get());
		final String st = AtmosRequestHandler.generateSubtenant();
		SUBTENANT = new AtmosSubTenantHelper(reqConf, st);
		SUBTENANT.create("127.0.0.1");
		if(!SUBTENANT.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the subtenant for test");
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.setAuthToken(st)
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		SUBTENANT.delete(BasicConfig.THREAD_CONTEXT.get().getStorageHttpAddrs()[0]);
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
