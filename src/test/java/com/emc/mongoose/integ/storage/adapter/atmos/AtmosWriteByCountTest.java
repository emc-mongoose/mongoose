package com.emc.mongoose.integ.storage.adapter.atmos;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.DataItemDst;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
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
	private final static List<WSObject> BUFF_WRITE = new ArrayList<>(COUNT_TO_WRITE);
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
			final StorageClient<WSObject> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.build()
		) {
			final DataItemDst<WSObject> writeOutput = new ListItemDst<>(BUFF_WRITE);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("8KB")
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
