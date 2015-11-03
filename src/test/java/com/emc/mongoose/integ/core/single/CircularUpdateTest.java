package com.emc.mongoose.integ.core.single;

import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.core.impl.data.model.CSVFileItemDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.integ.suite.StdOutInterceptorTestSuite;
import com.emc.mongoose.integ.tools.BufferingOutputStream;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;


/**
 * Created by gusakk on 30.10.15.
 */
public class CircularUpdateTest
extends StandaloneClientTestBase {
	//
	private static final int ITEM_MAX_QUEUE_SIZE = 65536;
	private static final int BATCH_SIZE = 100;
	//
	private static final int WRITE_COUNT = 1234;
	private static final int UPDATE_COUNT = 12340;
	//
	private static final int COUNT_OF_UPDATES = 10;
	//
	private static long COUNT_WRITTEN, COUNT_UPDATED;
	private static byte[] STD_OUT_CONTENT;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, CircularUpdateTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.set(RunTimeConfig.KEY_ITEM_SRC_CIRCULAR, true);
		rtConfig.set(RunTimeConfig.KEY_ITEM_QUEUE_MAX_SIZE, ITEM_MAX_QUEUE_SIZE);
		rtConfig.set(RunTimeConfig.KEY_ITEM_SRC_BATCH_SIZE, BATCH_SIZE);
		RunTimeConfig.setContext(rtConfig);
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setAPI("s3")
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(WRITE_COUNT)
				.build()
		) {
			final ItemDst<WSObject> writeOutput = new CSVFileItemDst<WSObject>(
				BasicWSObject.class, ContentSourceBase.getDefault()
			);
			COUNT_WRITTEN = client.write(
				null, writeOutput, WRITE_COUNT, 1, SizeUtil.toSize("128B")
			);
			TimeUnit.SECONDS.sleep(10);
			//
			try (
				final BufferingOutputStream
					stdOutInterceptorStream = StdOutInterceptorTestSuite.getStdOutBufferingStream()
			) {
				stdOutInterceptorStream.reset();
				if (COUNT_WRITTEN > 0) {
					COUNT_UPDATED = client.update(writeOutput.getItemSrc(), null, UPDATE_COUNT, 10, 1);
				} else {
					throw new IllegalStateException("Failed to write");
				}
				TimeUnit.SECONDS.sleep(1);
				STD_OUT_CONTENT = stdOutInterceptorStream.toByteArray();
			}
		}
		//
		RunIdFileManager.flushAll();
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		StdOutInterceptorTestSuite.reset();
		StandaloneClientTestBase.tearDownClass();
	}
	//
	@Test
	public void checkUpdatedCount() {
		Assert.assertEquals(COUNT_WRITTEN * COUNT_OF_UPDATES, COUNT_UPDATED);
	}

}