package com.emc.mongoose.integ.storage.adapter.atmos;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.storage.mock.impl.web.request.AtmosRequestHandler;
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
	private static SubTenant SUBTENANT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, AtmosUsePreExistingSubtenantTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final WSRequestConfigImpl reqConf = (WSRequestConfigImpl) WSRequestConfigBase
			.newInstanceFor("atmos")
			.setProperties(rtConfig);
		reqConf.setProperties(RunTimeConfig.getContext());
		SUBTENANT = new WSSubTenantImpl(
			reqConf, AtmosRequestHandler.generateSubtenant()
		);
		SUBTENANT.create("127.0.0.1");
		if(!SUBTENANT.exists("127.0.0.1")) {
			Assert.fail("Failed to pre-create the subtenant for test");
		}
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.setAtmosSubtenant(SUBTENANT.getValue())
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
		SUBTENANT.delete(RunTimeConfig.getContext().getStorageAddrs()[0]);
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
