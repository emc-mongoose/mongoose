package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.system.base.DistributedClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
//
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByTimeTest
extends DistributedClientTestBase {
	//
	private final static long TIME_TO_WRITE_SEC = 50;
	//
	private static long countWritten, timeActualSec;
	//
	@BeforeClass
	public static  void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, WriteByTimeTest.class.getCanonicalName());
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(TIME_TO_WRITE_SEC, TimeUnit.SECONDS)
				.setLimitCount(0)
				.setAPI("atmos")
				.build()
		) {
			timeActualSec = System.currentTimeMillis() / 1000;
			countWritten = client.write(null, null, 0, 10, SizeInBytes.toFixedSize("10KB"));
			timeActualSec = System.currentTimeMillis() / 1000 - timeActualSec;
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test public void checkRunTimeNotLessThanLimit() {
		Assert.assertTrue(timeActualSec >= TIME_TO_WRITE_SEC);
	}
	//
	@Test public void checkRunTimeNotMuchBiggerThanLimit() {
		Assert.assertTrue(timeActualSec <= TIME_TO_WRITE_SEC + 10);
	}
	//
	@Test public void checkSomethingWasWritten() {
		Assert.assertTrue(countWritten > 0);
	}
}
