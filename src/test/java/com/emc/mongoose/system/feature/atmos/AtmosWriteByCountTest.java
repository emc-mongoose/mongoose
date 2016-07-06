package com.emc.mongoose.system.feature.atmos;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.ListItemOutput;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosWriteByCountTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 12345;
	private final static List<HttpDataItem> BUFF_WRITE = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long COUNT_WRITTEN;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			AppConfig.KEY_RUN_ID, AtmosWriteByCountTest.class.getCanonicalName()
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
			final Output<HttpDataItem> writeOutput = new ListItemOutput<>(BUFF_WRITE);
			COUNT_WRITTEN = client.create(
				writeOutput, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("8KB")
			);
			//
			RunIdFileManager.flush(AtmosWriteByCountTest.class.getCanonicalName());
		}
	}
	//
	@Test
	public void checkWrittenItemsCount() {
		Assert.assertEquals(COUNT_TO_WRITE, COUNT_WRITTEN);
	}
}
