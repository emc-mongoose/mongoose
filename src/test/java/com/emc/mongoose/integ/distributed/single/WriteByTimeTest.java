package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.api.StorageClientBuilder;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByTimeTest {
	//
	private final static long TIME_TO_WRITE_SEC = 100;
	//
	private static long COUNT_WRITTEN, TIME_ACTUAL_SEC;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
					.setLimitTime(TIME_TO_WRITE_SEC, TimeUnit.SECONDS)
					.setLimitCount(0)
					.setClientMode(new String[] {ServiceUtils.getHostAddr()})
					.build()
		) {
			TIME_ACTUAL_SEC = System.currentTimeMillis() / 1000;
			COUNT_WRITTEN = client.write(null, null, (short) 10, SizeUtil.toSize("10KB"));
			TIME_ACTUAL_SEC = System.currentTimeMillis() / 1000 - TIME_ACTUAL_SEC;
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}
	//
	@Test
	public void checkRunTimeNotLessThanLimit() {
		Assert.assertTrue(TIME_ACTUAL_SEC >= TIME_TO_WRITE_SEC);
	}
	//
	@Test
	public void checkRunTimeNotMuchBiggerThanLimit() {
		Assert.assertTrue(TIME_ACTUAL_SEC <= TIME_TO_WRITE_SEC + 10);
	}
}
