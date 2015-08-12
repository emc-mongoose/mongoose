package com.emc.mongoose.integ.base;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.WSObjectMock;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
//
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
/**
 Created by andrey on 13.08.15.
 */
public abstract class WSMockTestBase
extends LoggingTestBase {
	//
	private StorageMock<WSObjectMock> wsMock;
	private Thread wsMockThread;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestBase.setUpClass();
		RT_CONFIG.set(RunTimeConfig.KEY_STORAGE_MOCK_CAPACITY, 1000000);
		RT_CONFIG.set(RunTimeConfig.KEY_STORAGE_MOCK_CONTAINER_CAPACITY, 1000000);
		RT_CONFIG.set(RunTimeConfig.KEY_STORAGE_MOCK_HEAD_COUNT, 5); // listen ports 9020..9024
		RT_CONFIG.set(RunTimeConfig.KEY_STORAGE_MOCK_IO_THREADS_PER_SOCKET, 5);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		LoggingTestBase.tearDownClass();
	}
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		wsMock = new Cinderella<>(RT_CONFIG);
		wsMockThread = new Thread(wsMock, "wsMock");
		wsMockThread.setDaemon(true);
		wsMockThread.start();
		LOG.info(Markers.MSG, "Cinderella started");
	}
	//
	@After
	public void tearDown()
	throws Exception {
		wsMockThread.interrupt();
		wsMock.close();
		LOG.info(Markers.MSG, "Cinderella stopped");
		super.tearDown();
	}
}
