package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import org.apache.logging.log4j.LogManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by kurila on 14.07.15.
 */
public abstract class WSMockTestSuite
extends ConfiguredTestSuite {
	//
	private static StorageMock<WSObjectMock> WSMOCK;
	private static Thread WSMOCK_THREAD;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestSuite.setUpClass();
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_CAPACITY, 1000000);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, 1000000);
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_HEAD_COUNT, 5); // listen ports 9020..9024
		rtConfig.set(RunTimeConfig.KEY_STORAGE_MOCK_IO_THREADS_PER_SOCKET, 5);
		WSMOCK = new Cinderella<>(rtConfig);
		WSMOCK_THREAD = new Thread(WSMOCK, "wsMock");
		WSMOCK_THREAD.setDaemon(true);
		WSMOCK_THREAD.start();
		LogManager.getLogger().info(Markers.MSG, "Cinderella started");
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		WSMOCK_THREAD.interrupt();
		WSMOCK.close();
		LogManager.getLogger().info(Markers.MSG, "Cinderella stopped");
		ConfiguredTestSuite.tearDownClass();
	}
}
