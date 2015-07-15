package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.net.ServiceUtils;
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
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByCountTest {
	//
	private final static long COUNT_TO_WRITE = 100000;
	//
	private static StorageClient<WSObject> CLIENT;
	private static long COUNT_WRITTEN;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_LOAD_LIMIT_COUNT, COUNT_TO_WRITE);
		final StorageClientBuilder<WSObject, StorageClient<WSObject>>
			clientBuilder = new BasicWSClientBuilder<>();
		CLIENT = clientBuilder
			.setClientMode(new String[] {ServiceUtils.getHostAddr()})
			.build();
		COUNT_WRITTEN = CLIENT.write(null, null, (short) 10, SizeUtil.toSize("10KB"));
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		CLIENT.close();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
	}
}
