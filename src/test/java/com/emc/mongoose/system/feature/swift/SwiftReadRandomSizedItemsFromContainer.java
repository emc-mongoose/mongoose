package com.emc.mongoose.system.feature.swift;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 06.08.15.
 */
public class SwiftReadRandomSizedItemsFromContainer
extends StandaloneClientTestBase {
	//
	private final static long COUNT_TO_WRITE = 10000;
	//
	private static long COUNT_WRITTEN, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		final String runId = SwiftReadRandomSizedItemsFromContainer.class.getCanonicalName();
		System.setProperty(AppConfig.KEY_RUN_ID, runId);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("swift")
				.setNamespace("swift")
				.setDstContainer(runId)
				.build()
		) {
			COUNT_WRITTEN = client.create(null, COUNT_TO_WRITE, 10, 0, 123456, 3);
			RunIdFileManager.flushAll();
		}
		//
		try(
			final StorageClient<HttpDataItem> client = CLIENT_BUILDER
		        .setSrcContainer(runId)
		        .build();
		) {
			if(COUNT_WRITTEN > 0) {
				COUNT_READ = client.read(null, null, COUNT_WRITTEN, 10, true);
			} else {
				throw new IllegalStateException("Failed to write");
			}

		}
	}
	//
	@Test
	public void checkReadCount() {
		Assert.assertEquals(COUNT_WRITTEN, COUNT_TO_WRITE);
		Assert.assertEquals(COUNT_WRITTEN, COUNT_READ);
	}
}
