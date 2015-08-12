package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.net.ServiceUtils;
import com.emc.mongoose.core.api.data.WSObject;
//
import com.emc.mongoose.core.impl.io.req.WSRequestConfigBase;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.storage.adapter.s3.Bucket;
import com.emc.mongoose.storage.adapter.s3.WSBucketImpl;
import com.emc.mongoose.storage.adapter.s3.WSRequestConfigImpl;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteByCountTest
extends DistributedClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 100000;
	//
	private long countWritten;
	//
	@Before
	public void setUp()
	throws Exception {
		super.setUp();
		try(
			final StorageClient<WSObject> client = clientBuilder
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.build()
		) {
			countWritten = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
		}
	}
	//
	@After
	public void tearDown()
	throws Exception {
		final Bucket bucket = new WSBucketImpl(
			(WSRequestConfigImpl) WSRequestConfigBase.newInstanceFor("s3").setProperties(RT_CONFIG),
			RT_CONFIG.getString(RunTimeConfig.KEY_API_S3_BUCKET), false
		);
		bucket.delete(RT_CONFIG.getStorageAddrs()[0]);
		super.tearDown();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(countWritten, COUNT_TO_WRITE);
	}
}
