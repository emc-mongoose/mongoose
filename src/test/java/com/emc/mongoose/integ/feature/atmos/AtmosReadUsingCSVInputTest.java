package com.emc.mongoose.integ.feature.atmos;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.impl.data.BasicWSObject;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.core.impl.data.model.ItemCSVFileDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosReadUsingCSVInputTest
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
			RunTimeConfig.KEY_RUN_ID, AtmosReadUsingCSVInputTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.build()
		) {
			final ItemDst<WSObject> writeOutput = new ItemCSVFileDst<>(
				(Class) BasicWSObject.class, ContentSourceBase.getDefault()
			);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("10MB")
			);
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(writeOutput.getItemSrc(), null, COUNT_WRITTEN, 10, true);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkWrittenItemsCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
