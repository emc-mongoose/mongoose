package com.emc.mongoose.integ.core.single;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
import com.emc.mongoose.core.impl.data.model.ListItemSrc;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 19.10.15.
 */
public class UpdateEqualBytesLayerSwitchTest
extends StandaloneClientTestBase {
	private final static int COUNT_TO_WRITE = 1000, OBJ_SIZE = (int) SizeUtil.toSize("1KB");
	private final static String
		RUN_ID = UpdateEqualBytesLayerSwitchTest.class.getCanonicalName(),
		BASE_URL = "http://127.0.0.1:9020/" + UpdateEqualBytesLayerSwitchTest.class.getSimpleName() + "/";
	private final static List<WSObject>
		OBJ_BUFF_WRITTEN = new ArrayList<>(COUNT_TO_WRITE),
		OBJ_BUFF_UPDATED_1 = new ArrayList<>(COUNT_TO_WRITE),
		OBJ_BUFF_UPDATED_2 = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static int countWritten, countUpdated1, countUpdated2, countRead;
	//
	@BeforeClass
	public static void setUpClass()
		throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_DATA_CONTENT_FPATH, "conf/content/equalbytes");
		StandaloneClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(UpdateZeroBytesTest.class.getSimpleName())
				.build()
		) {
			countWritten = (int) client.write(
				null, new ListItemDst<>(OBJ_BUFF_WRITTEN), COUNT_TO_WRITE, 10, OBJ_SIZE
			);
			countUpdated1 = (int) client.update(
				new ListItemSrc<>(OBJ_BUFF_WRITTEN), new ListItemDst<>(OBJ_BUFF_UPDATED_1),
				countWritten, 10, 10
			);
			countUpdated2 = (int) client.update(
				new ListItemSrc<>(OBJ_BUFF_UPDATED_1), new ListItemDst<>(OBJ_BUFF_UPDATED_2),
				countUpdated1, 10, 10
			);
			countRead = (int) client.read(
				new ListItemSrc<>(OBJ_BUFF_UPDATED_2), null, countUpdated2, 10, true
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countRead);
	}
}
