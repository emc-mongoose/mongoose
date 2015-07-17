package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.storage.mock.api.Storage;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
import com.emc.mongoose.storage.mock.impl.web.data.BasicWSObjectMock;
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
	private static Storage<BasicWSObjectMock> WSMOCK;
	private static Thread WSMOCK_THREAD;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		ConfiguredTestSuite.setUpClass();
		WSMOCK = new Cinderella(RunTimeConfig.getContext());
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
