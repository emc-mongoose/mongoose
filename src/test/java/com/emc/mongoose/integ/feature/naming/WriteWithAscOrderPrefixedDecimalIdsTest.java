package com.emc.mongoose.integ.feature.naming;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.base.ItemNamingType;
import com.emc.mongoose.core.api.item.data.WSObject;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 15.02.16.
 */
public class WriteWithAscOrderPrefixedDecimalIdsTest
extends DistributedClientTestBase {
	private final static String
		RUN_ID = WriteWithAscOrderPrefixedDecimalIdsTest.class.getCanonicalName(),
		PREFIX = "yohoho";
	private final static int COUNT_TO_WRITE = 6789, OBJ_SIZE = (int) SizeUtil.toSize("1KB");
	private final static List<WSObject> OBJ_BUFF = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, PREFIX);
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_LENGTH, "16");
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_TYPE, ItemNamingType.ASC.name());
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_RADIX, "10");
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(WriteWithPrefixAndFixedLengthTest.class.getSimpleName())
				.build()
		) {
			countWritten = client.write(
				null, new ListItemDst<>(OBJ_BUFF), COUNT_TO_WRITE, 10, OBJ_SIZE
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, "");
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_LENGTH, "13");
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_TYPE, ItemNamingType.RANDOM.name());
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_RADIX, Integer.toString(Character.MAX_RADIX));
		DistributedClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
	}
	//
	@Test
	public void checkAllObjectsNamesAreOk()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		String name; long id;
		for(final WSObject wso : OBJ_BUFF) {
			name = wso.getName();
			Assert.assertTrue("Object name length is incorrect", name.length() == 16);
			Assert.assertTrue("Object name should have prefix \"" + PREFIX + "\"", name.startsWith(PREFIX));
			id = Long.valueOf(name.substring(PREFIX.length()), 10);
			Assert.assertFalse(id < 0);
		}
	}
}
