package com.emc.mongoose.integ.core.api.swift;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.storage.adapter.swift.AuthToken;
import com.emc.mongoose.storage.adapter.swift.Container;
import com.emc.mongoose.storage.adapter.swift.WSAuthTokenImpl;
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
public class SwiftUsePreExistingAuthTokenTest {
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
		RunTimeConfig.getContext().set(
			RunTimeConfig.KEY_RUN_ID, SwiftUsePreExistingAuthTokenTest.class.getCanonicalName()
		);
		//
		final WSRequestConfigImpl reqConf = new WSRequestConfigImpl();
		reqConf.setProperties(RunTimeConfig.getContext());
		AUTH_TOKEN = new WSAuthTokenImpl(
			reqConf, SwiftUsePreExistingContainerTest.class.getSimpleName()
		);
		AUTH_TOKEN.create("127.0.0.1");
		//
		try(
			final StorageClient<WSObject> client = new BasicWSClientBuilder<>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setSwiftAuthToken(AUTH_TOKEN.getValue())
				.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		final Container container = new WSContainerImpl(
			(WSRequestConfigImpl) WSLoadBuilderFactory.getInstance(rtConfig).getRequestConfig(),
			rtConfig.getString(RunTimeConfig.KEY_API_SWIFT_CONTAINER), false
		);
		container.delete(rtConfig.getStorageAddrs()[0]);
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
