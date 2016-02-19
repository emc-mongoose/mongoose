package com.emc.mongoose.integ.feature.atmos;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
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
			RunTimeConfig.KEY_RUN_ID, AtmosWriteByCountTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.build()
		) {
			final ItemDst<HttpDataItem> writeOutput = new ListItemDst<>(BUFF_WRITE);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("8KB")
			);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@Test
	public void checkWrittenItemsCount() {
		Assert.assertEquals(COUNT_TO_WRITE, COUNT_WRITTEN);
	}
}
