package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
//
import org.junit.Assert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByCountTest {
	//
	private final static long COUNT_TO_WRITE = 100000;
	//
	private static long COUNT_WRITTEN;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setClientMode(new String[] {ServiceUtils.getHostAddr()})
					.build()
		) {
			COUNT_WRITTEN = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
