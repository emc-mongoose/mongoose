package com.emc.mongoose.integ.feature.naming;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
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
public class WriteWithPrefixAndFixedLengthTest
extends StandaloneClientTestBase {
	private final static String RUN_ID = WriteWithPrefixAndFixedLengthTest.class.getCanonicalName();
	private final static int
		COUNT_TO_WRITE = 100, OBJ_SIZE = (int) SizeUtil.toSize("4KB"),
		NAME_LENGTH = RUN_ID.length() + 3;
	private final static List<WSObject> OBJ_BUFF = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_LENGTH, Integer.toString(NAME_LENGTH));
		StandaloneClientTestBase.setUpClass();
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
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_TYPE, AppConfig.ItemNamingType.RANDOM.name());
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
	public void checkAllObjectsHaveThePrefix()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		for(final WSObject wso : OBJ_BUFF) {
			Assert.assertTrue(wso.getName().startsWith(RUN_ID));
		}
	}
	//
	//
	@Test
	public void checkAllObjectNamesHaveTheProperLength()
	throws Exception {
		Assert.assertEquals(COUNT_TO_WRITE, OBJ_BUFF.size());
		for(final WSObject wso : OBJ_BUFF) {
			Assert.assertEquals(NAME_LENGTH, wso.getName().length());
		}
	}
}
