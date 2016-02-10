package com.emc.mongoose.integ.cambridgelab;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.container.BasicContainer;
import com.emc.mongoose.core.impl.item.base.ListItemSrc;
import com.emc.mongoose.integ.base.CambridgeLabDistributedClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.apache.logging.log4j.Level;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class S3WriteByCountTest
extends CambridgeLabDistributedClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	private final static String RUN_ID = S3WriteByCountTest.class.getCanonicalName();
	//
	private static long countWritten;
	//
	@BeforeClass
	public static void setUpClass() {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		try {
			CambridgeLabDistributedClientTestBase.setUpClass();
			try(
				final StorageClient client = CLIENT_BUILDER
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setAPI("s3")
					.setS3Bucket(RUN_ID)
					.build()
			) {
				countWritten = client.write(
					null, null, COUNT_TO_WRITE, 100, SizeInBytes.toFixedSize("100KB")
				);
				//
				RunIdFileManager.flushAll();
			}
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Preconditions failure");
		}
	}
	//
	@AfterClass
	public static void tearDownClass() {
		//
		try(
			final StorageClient client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("s3")
				.setS3Bucket(RUN_ID)
				.build()
		) {
			client.delete(null, null, COUNT_TO_WRITE, 100);
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Preconditions failure");
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
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Preconditions failure");
		}
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten, COUNT_TO_WRITE / 100);
	}
}
