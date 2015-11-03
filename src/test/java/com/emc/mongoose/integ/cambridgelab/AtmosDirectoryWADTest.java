package com.emc.mongoose.integ.cambridgelab;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
import com.emc.mongoose.integ.base.CambridgeLabDistributedClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.11.15.
 */
public class AtmosDirectoryWADTest
	extends CambridgeLabDistributedClientTestBase {
	//
	private final static String RUN_ID = S3DirectoryWRDTest.class.getCanonicalName();
	private final static long COUNT_LIMIT = 1000;
	//
	private static long countWritten, countAppended, countDeleted;
	//
	@BeforeClass
	public static void setUpClass() {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		try {
			CambridgeLabDistributedClientTestBase.setUpClass();
			try(
				final StorageClient client = CLIENT_BUILDER
					.setLimitTime(100, TimeUnit.SECONDS)
					.setAPI("atmos")
					.setFileAccess(true)
					.setPath("a/b/c")
					.build()
			) {
				try(
					final ItemDst<WSObject>
						writtenItems = new ListItemDst<>(new ArrayList<WSObject>())
				) {
					countWritten = client.write(
						null, writtenItems, COUNT_LIMIT, 10, SizeUtil.toSize("8KB")
					);
					TimeUnit.SECONDS.sleep(1);
					countAppended = client.append(
						writtenItems.getItemSrc(), null, countWritten, 10, SizeUtil.toSize("2KB")
					);
					TimeUnit.SECONDS.sleep(1);
					countDeleted = client.delete(
						writtenItems.getItemSrc(), null, countWritten, 10
					);
					RunIdFileManager.flushAll();
				}
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Preconditions failure");
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertTrue(countWritten > 0);
		Assert.assertTrue(countAppended > 0);
		Assert.assertTrue(countDeleted > 0);
		Assert.assertTrue(countWritten >= countAppended);
		Assert.assertTrue(countWritten >= countDeleted);
	}
	//
	@AfterClass
	public static void tearDownClass() {
		try {
			CambridgeLabDistributedClientTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Postconditions failure");
		}
	}

}
