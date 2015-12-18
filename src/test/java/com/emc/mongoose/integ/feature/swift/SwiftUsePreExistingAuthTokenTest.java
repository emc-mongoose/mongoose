package com.emc.mongoose.integ.feature.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.impl.io.conf.WSRequestConfigBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.adapter.swift.AuthToken;
import com.emc.mongoose.storage.adapter.swift.WSAuthTokenImpl;
import com.emc.mongoose.storage.adapter.swift.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class SwiftUsePreExistingAuthTokenTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN;
	private static AuthToken AUTH_TOKEN;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, SwiftUsePreExistingAuthTokenTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		final WSRequestConfigImpl
			reqConf = (WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("swift");
		reqConf.setRunTimeConfig(RunTimeConfig.getContext());
		AUTH_TOKEN = new WSAuthTokenImpl(
			reqConf, SwiftUsePreExistingContainerTest.class.getSimpleName()
		);
		AUTH_TOKEN.create("127.0.0.1");
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftAuthToken(AUTH_TOKEN.getValue())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
