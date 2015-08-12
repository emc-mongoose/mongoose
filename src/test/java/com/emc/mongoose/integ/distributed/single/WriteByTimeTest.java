package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.storage.adapter.atmos.SubTenant;
import com.emc.mongoose.storage.adapter.atmos.WSRequestConfigImpl;
import com.emc.mongoose.storage.adapter.atmos.WSSubTenantImpl;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.junit.After;
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByTimeTest
extends DistributedClientTestBase {
	//
	private final static long TIME_TO_WRITE_SEC = 100;
	//
	private long countWritten, timeActualSec;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		try(
			final StorageClient<WSObject> client = clientBuilder
				.setLimitTime(TIME_TO_WRITE_SEC, TimeUnit.SECONDS)
				.setLimitCount(0)
				.setAPI("atmos")
				.build()
		) {
			timeActualSec = System.currentTimeMillis() / 1000;
			countWritten = client.write(null, null, 0, 10, SizeUtil.toSize("10KB"));
			timeActualSec = System.currentTimeMillis() / 1000 - timeActualSec;
		}
	}
	//
	@After
	public void tearDown()
	throws Exception {
		final SubTenant st = new WSSubTenantImpl(
			(WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("atmos").setProperties(RT_CONFIG),
			RT_CONFIG.getString(RunTimeConfig.KEY_API_ATMOS_SUBTENANT)
		);
		st.delete(RT_CONFIG.getStorageAddrs()[0]);
		super.tearDown();
	}
	//
	@Test
	public void checkRunTimeNotLessThanLimit() {
		Assert.assertTrue(timeActualSec >= TIME_TO_WRITE_SEC);
	}
	//
	@Test
	public void checkRunTimeNotMuchBiggerThanLimit() {
		Assert.assertTrue(timeActualSec <= TIME_TO_WRITE_SEC + 10);
	}
	//
	@Test
	public void checkSomethingWasWritten() {
		Assert.assertTrue(countWritten > 0);
	}
}
