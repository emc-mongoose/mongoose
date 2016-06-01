package com.emc.mongoose.system.feature.naming;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.ItemListOutput;
import com.emc.mongoose.system.base.DistributedClientTestBase;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
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
public class WriteWithPrefixAndFixedLengthTest
extends StandaloneClientTestBase {
	private final static String RUN_ID = WriteWithPrefixAndFixedLengthTest.class.getCanonicalName();
	private final static int
		COUNT_TO_WRITE = 100, OBJ_SIZE = (int) SizeInBytes.toFixedSize("4KB"),
		NAME_LENGTH = RUN_ID.length() + 3;
	private final static List<HttpDataItem> OBJ_BUFF = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_NAMING_PREFIX, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_NAMING_LENGTH, Integer.toString(NAME_LENGTH));
		StandaloneClientTestBase.setUpClass();
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setDstContainer(WriteWithPrefixAndFixedLengthTest.class.getSimpleName())
				.build()
		) {
			countWritten = client.create(
				new ItemListOutput<>(OBJ_BUFF), COUNT_TO_WRITE, 10, OBJ_SIZE
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
	public void checkAllObjectsHaveThePrefix()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		for(final HttpDataItem wso : OBJ_BUFF) {
			Assert.assertTrue(wso.getName().startsWith(RUN_ID));
		}
	}
	//
	//
	@Test
	public void checkAllObjectNamesHaveTheProperLength()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		for(final HttpDataItem wso : OBJ_BUFF) {
			Assert.assertEquals(NAME_LENGTH, wso.getName().length());
		}
	}
}
