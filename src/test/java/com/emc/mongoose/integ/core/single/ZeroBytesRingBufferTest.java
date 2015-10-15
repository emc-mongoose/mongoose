package com.emc.mongoose.integ.core.single;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.storage.mock.api.ObjectContainerMock;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 15.10.15.
 */
public class ZeroBytesRingBufferTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 1000;
	private final static List<WSObject> writtenItems = new ArrayList<>(COUNT_TO_WRITE);
	private final static String RUN_ID = ZeroBytesRingBufferTest.class.getCanonicalName();
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_DATA_SRC_RING_SEED, "0000000000000000");
		System.setProperty(RunTimeConfig.KEY_DATA_SRC_RING_UNIFORM_SIZE, "8");
		StandaloneClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setS3Bucket(ObjectContainerMock.DEFAULT_NAME)
				.build()
		) {
			countWritten = client.write(
				null, new ListItemDst<>(writtenItems), COUNT_TO_WRITE, 10, SizeUtil.toSize("8KB")
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkRingBufferContainsZeroBytesOnly() {
		final ByteBuffer ringBuff = UniformDataSource.DEFAULT.getLayer(0);
		for(int i = 0; i < ringBuff.capacity(); i ++) {
			Assert.assertEquals(
				"Byte #" + i + " is not zero: " + Integer.toHexString(ringBuff.get(i)),
				ringBuff.get(i), (byte) 0
			);
		}
	}
	//
	@Test
	public void checkAllDataItemsContainZeroBytesOnly()
	throws Exception {
		final String baseObjUri = "http://127.0.0.1:9020/" + ObjectContainerMock.DEFAULT_NAME + "/";
		WSObject nextObj;
		URL nextObjURL;
		final int buffSize = (int) SizeUtil.toSize("8KB");
		final byte buff[] = new byte[buffSize];
		Assert.assertEquals(countWritten, writtenItems.size());
		for(int i = 0; i < countWritten; i ++) {
			nextObj = writtenItems.get(i);
			Assert.assertNotNull(
				"Looks like the output objects buffer contains less than " + countWritten + " elements: " + i,
				nextObj
			);
			nextObjURL = new URL(baseObjUri + nextObj.getId());
			try(
				final BufferedInputStream in = new BufferedInputStream(
					nextObjURL.openStream(), buffSize
				)
			) {
				int n = 0, m;
				do {
					m = in.read(buff, n, buffSize - n);
					if(m < 0) {
						Assert.fail("Was able to read only " + n + " bytes for \"" + nextObjURL + "\"");
					} else {
						n += m;
					}
				} while(n < buffSize);
			}
			//
			for(int j = 0; j < buffSize; j ++) {
				Assert.assertEquals(
					"Byte #" + j + " of the \"" + nextObj.getId() + "\" is not zero: " + Integer.toHexString(buff[j]),
					buff[j], (byte) 0
				);
			}
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
	}
}
