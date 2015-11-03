package com.emc.mongoose.integ.cambridgelab;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
//
import com.emc.mongoose.core.impl.container.BasicContainer;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
import com.emc.mongoose.core.impl.data.model.ListItemSrc;
//
import com.emc.mongoose.integ.base.CambridgeLabDistributedClientTestBase;
//
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.11.15.
 */
public class Read100MBTest
extends CambridgeLabDistributedClientTestBase {
	//
	private final static String RUN_ID = WriteByCountTest.class.getCanonicalName();
	//
	private static long countWritten, countRead;
	//
	@BeforeClass
	public static void setUpClass() {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		try {
			CambridgeLabDistributedClientTestBase.setUpClass();
			try(
				final StorageClient client = CLIENT_BUILDER
					.setLimitTime(100, TimeUnit.SECONDS)
					.setAPI("s3")
					.setS3Bucket(RUN_ID)
					.build()
			) {
				try(
					final ItemDst<WSObject>
						writtenItems = new ListItemDst<>(new ArrayList<WSObject>())
				) {
					countWritten = client.write(null, writtenItems, 0, 100, SizeUtil.toSize("100MB"));
					countRead = client.read(writtenItems.getItemSrc(), null, countWritten, 100, true);
					RunIdFileManager.flushAll();
				}
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Preconditions failure");
		}
	}
	//
	@AfterClass
	public static void tearDownClass() {
		try(
			final StorageClient client = CLIENT_BUILDER
				.setLimitTime(100, TimeUnit.SECONDS)
				.setAPI("s3")
				.setS3Bucket(RUN_ID)
				.build()
		) {
			client.delete(null, null, countWritten, 100);
		} catch(final IOException | InterruptedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Postconditions failure");
		}
		//
		try(
			final StorageClient client = CLIENT_BUILDER
				.setLimitTime(100, TimeUnit.SECONDS)
				.setItemClass("container")
				.build()
		) {
			final List<Container<WSObject>> containers2delete = new ArrayList<>();
			containers2delete.add(new BasicContainer<WSObject>(RUN_ID));
			client.delete(new ListItemSrc<>(containers2delete), null, countWritten, 100);
		} catch(final IOException | InterruptedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Postconditions failure");
		}
		//
		try {
			CambridgeLabDistributedClientTestBase.tearDownClass();
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Postconditions failure");
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertTrue(countWritten > 0);
		Assert.assertTrue(countRead > 0);
		Assert.assertTrue(countWritten >= countRead);
	}
}
