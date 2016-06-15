package com.emc.mongoose.system.feature.atmos;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.CsvFileItemOutput;
import com.emc.mongoose.core.impl.item.data.BasicHttpData;
import com.emc.mongoose.core.impl.item.data.ContentSourceUtil;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosReadUsingCsvInputTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 1000;
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			AppConfig.KEY_RUN_ID, AtmosReadUsingCsvInputTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.setAuth("wuser1@sanity.local", null)
				.build()
		) {
			final Output<HttpDataItem> writeOutput = new CsvFileItemOutput<>(
				(Class) BasicHttpData.class, ContentSourceUtil.getDefaultInstance()
			);
			COUNT_WRITTEN = client.create(
				writeOutput, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("10MB")
			);
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(writeOutput.getInput(), null, COUNT_WRITTEN, 10, true);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			//
			RunIdFileManager.flush(AtmosReadUsingCsvInputTest.class.getCanonicalName());
		}
	}
	//
	@Test
	public void checkWrittenItemsCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
