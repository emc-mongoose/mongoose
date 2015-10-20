package com.emc.mongoose.integ.distributed.single;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.integ.base.DistributedClientTestBase;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
//
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
public final class WriteByCountTest
extends DistributedClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 100000;
	private final static String RUN_ID = WriteByCountTest.class.getCanonicalName();
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		DistributedClientTestBase.setUpClass();
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.build()
		) {
			countWritten = client.write(null, null, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB"));
			//
			RunIdFileManager.flushAll();
		}
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
				LogValidator.getDataItemsFile(RUN_ID).toPath(), StandardCharsets.UTF_8
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
