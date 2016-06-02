package com.emc.mongoose.system.feature.naming;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.ListItemOutput;
import com.emc.mongoose.system.base.DistributedClientTestBase;
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
	private final static int COUNT_TO_WRITE = 6789, OBJ_SIZE = (int) SizeInBytes.toFixedSize("1KB");
	private final static List<HttpDataItem> OBJ_BUFF = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_NAMING_PREFIX, PREFIX);
		System.setProperty(AppConfig.KEY_ITEM_NAMING_LENGTH, "16");
		System.setProperty(AppConfig.KEY_ITEM_NAMING_TYPE, ItemNamingType.ASC.name());
		System.setProperty(AppConfig.KEY_ITEM_NAMING_RADIX, "10");
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setDstContainer(WriteWithPrefixAndFixedLengthTest.class.getSimpleName())
				.build()
		) {
			countWritten = client.create(
				new ListItemOutput<>(OBJ_BUFF), COUNT_TO_WRITE, 10, OBJ_SIZE
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_ITEM_NAMING_PREFIX, "");
		System.setProperty(AppConfig.KEY_ITEM_NAMING_LENGTH, "13");
		System.setProperty(AppConfig.KEY_ITEM_NAMING_TYPE, ItemNamingType.RANDOM.name());
		System.setProperty(AppConfig.KEY_ITEM_NAMING_RADIX, Integer.toString(Character.MAX_RADIX));
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
		for(final HttpDataItem wso : OBJ_BUFF) {
			name = wso.getName();
			Assert.assertTrue("Object name length is incorrect", name.length() == 16);
			Assert.assertTrue("Object name should have prefix \"" + PREFIX + "\"", name.startsWith(PREFIX));
			id = Long.valueOf(name.substring(PREFIX.length()), 10);
			Assert.assertFalse(id < 0);
		}
	}
}
