package com.emc.mongoose.integ.feature.content;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.core.impl.item.base.ListItemSrc;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.EOFException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by andrey on 16.10.15.
 */
public class UpdateZeroBytesDistributedTest
extends DistributedClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 100, OBJ_SIZE = (int) SizeUtil.toSize("100KB");
	private final static String
		RUN_ID = UpdateZeroBytesDistributedTest.class.getCanonicalName(),
		BASE_URL = "http://127.0.0.1:9020/" + UpdateZeroBytesDistributedTest.class.getSimpleName() + "/";
	private final static List<WSObject>
		OBJ_BUFF_WRITTEN = new ArrayList<>(COUNT_TO_WRITE),
		OBJ_BUFF_UPDATED = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static int countWritten, countUpdated;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_DATA_CONTENT_FPATH, "conf/content/zerobytes");
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(UpdateZeroBytesDistributedTest.class.getSimpleName())
				.build()
		) {
			countWritten = (int) client.write(
				null, new ListItemDst<>(OBJ_BUFF_WRITTEN), COUNT_TO_WRITE, 10, OBJ_SIZE
			);
			countUpdated = (int) client.update(
				new ListItemSrc<>(OBJ_BUFF_WRITTEN), new ListItemDst<>(OBJ_BUFF_UPDATED),
				countWritten, 10, 16
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countUpdated);
	}
	// zero bytes update has no effect as far as xorshift of 0-word returns 0-word
	@Test
	public void checkAllUpdatedContainsNonZeroBytes()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF_UPDATED.size());
		URL nextObjURL;
		final byte buff[] = new byte[OBJ_SIZE];
		WSObject nextObj;
		for(int i = 0; i < OBJ_BUFF_UPDATED.size(); i ++) {
			nextObj = OBJ_BUFF_UPDATED.get(i);
			nextObjURL = new URL(BASE_URL + nextObj.getName());
			try(final BufferedInputStream in = new BufferedInputStream(nextObjURL.openStream())) {
				int n = 0, m;
				do {
					m = in.read(buff, n, OBJ_SIZE - n);
					if(m < 0) {
						throw new EOFException(
							"#" + i + ": unexpected end of stream @ offset " + n +
								" while reading the content from " + nextObjURL
						);
					} else {
						n += m;
					}
				} while(n < OBJ_SIZE);
				//
				boolean nonZeroByte = false;
				for(int j = 0; j < OBJ_SIZE; j ++) {
					nonZeroByte = buff[j] != 0;
					if(nonZeroByte) {
						break;
					}
				}
				Assert.assertTrue(
					"Non-zero bytes have not been found in the " + i +
						"th updated object: " + nextObj.toString(),
					nonZeroByte
				);
			}
		}
	}
}
