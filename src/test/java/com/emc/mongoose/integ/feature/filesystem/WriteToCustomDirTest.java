package com.emc.mongoose.integ.feature.filesystem;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class WriteToCustomDirTest
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 100000;
	private final static String RUN_ID = WriteToCustomDirTest.class.getCanonicalName();
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID);
		StandaloneClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setItemClass("file")
				.build()
		) {
			countWritten = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "");
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
	}
	//
	@Test
	public void checkLoggedItemsCount()
	throws Exception {
		int itemsCount = 0;
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getItemsListFile(RUN_ID).toPath(), StandardCharsets.UTF_8
			)
		) {
			while(in.readLine() != null) {
				itemsCount ++;
			}
		}
		Assert.assertEquals(
			"Expected " + countWritten + " in the output CSV file, but got " + itemsCount,
			itemsCount, countWritten
		);
	}
}
