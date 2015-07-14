package com.emc.mongoose.integ;
//
import com.emc.mongoose.storage.mock.api.Storage;
import com.emc.mongoose.storage.mock.impl.web.Cinderella;
import com.emc.mongoose.storage.mock.impl.web.data.BasicWSObjectMock;
import org.junit.AfterClass;
import org.junit.BeforeClass;
/**
 Created by kurila on 14.07.15.
 */
public abstract class WSMockUsingTestBase
extends ConfiguredTestBase {
	//
	protected static Storage<BasicWSObjectMock> WSMOCK;
	protected static Thread WSMOCK_THREAD;
	//
	@BeforeClass
	public static void before()
	throws Exception {
		WSMOCK = new Cinderella(RUNTIME_CONFIG);
		WSMOCK_THREAD = new Thread(WSMOCK, "wsMock");
		WSMOCK_THREAD.setDaemon(true);
		WSMOCK_THREAD.start();
	}
	//
	@AfterClass
	public static void after()
	throws Exception {
		WSMOCK_THREAD.interrupt();
		WSMOCK.close();
	}
}
