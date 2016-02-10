package com.emc.mongoose.integ.cambridgelab;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
//
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.core.impl.item.base.ListItemSrc;
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
public class S3Read100MBTest
extends CambridgeLabDistributedClientTestBase {
	//
	private final static String RUN_ID = S3Read100MBTest.class.getCanonicalName();
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
					final ItemDst<HttpDataItem>
						writtenItems = new ListItemDst<>(new ArrayList<HttpDataItem>())
				) {
					countWritten = client.write(null, writtenItems, 0, 2, SizeInBytes.toFixedSize("100MB"));
					TimeUnit.SECONDS.sleep(1);
					countRead = client.read(writtenItems.getItemSrc(), null, countWritten, 2, true);
					TimeUnit.SECONDS.sleep(1);
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
			final List<Container<HttpDataItem>> containers2delete = new ArrayList<>();
			containers2delete.add(new BasicContainer<HttpDataItem>(RUN_ID));
			client.delete(new ListItemSrc<>(containers2delete), null, 1, 1);
			TimeUnit.SECONDS.sleep(1);
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
	}
}
