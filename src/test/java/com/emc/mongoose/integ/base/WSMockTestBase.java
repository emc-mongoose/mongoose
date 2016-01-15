package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import com.emc.mongoose.storage.mock.impl.web.Nagaina;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 13.08.15.
 */
public abstract class WSMockTestBase
extends LoggingTestBase {
	//
	private static StorageMock<WSObjectMock> WS_MOCK;
	private static Thread WS_MOCK_THREAD;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestBase.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_API_NAME, "s3"); // reset the ports beginning
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_HEAD_COUNT, 5); // listen ports 9020..9024
//		WS_MOCK = new Cinderella<>(rtConfig);
		WS_MOCK = new Nagaina<>(rtConfig);
		WS_MOCK_THREAD = new Thread(WS_MOCK, "wsMock");
		WS_MOCK_THREAD.setDaemon(true);
		WS_MOCK_THREAD.start();
		TimeUnit.SECONDS.sleep(1);
//		LOG.info(Markers.MSG, "Cinderella started");
		LOG.info(Markers.MSG, "Nagaina started");
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WS_MOCK_THREAD.interrupt();
		WS_MOCK.close();
//		LOG.info(Markers.MSG, "Cinderella stopped");
		LOG.info(Markers.MSG, "Nagaina stopped");
		LoggingTestBase.tearDownClass();
		ContentSourceBase.DEFAULT = null; // reset the content source
	}
}
