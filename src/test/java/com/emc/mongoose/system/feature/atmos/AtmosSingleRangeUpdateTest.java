package com.emc.mongoose.system.feature.atmos;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.impl.item.base.ItemListOutput;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosSingleRangeUpdateTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 10000;
	private final static List<HttpDataItem>
		BUFF_WRITE = new ArrayList<>(COUNT_TO_WRITE),
		BUFF_UPDATE = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long COUNT_WRITTEN, COUNT_UPDATED, COUNT_READ;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			AppConfig.KEY_RUN_ID, AtmosSingleRangeUpdateTest.class.getCanonicalName()
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
			final Output<HttpDataItem> writeOutput = new ItemListOutput<>(BUFF_WRITE);
			COUNT_WRITTEN = client.create(
				writeOutput, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("1KB")
			);
			final Output<HttpDataItem> updateOutput = new ItemListOutput<>(BUFF_UPDATE);
			if(COUNT_WRITTEN > 0) {
				COUNT_UPDATED = client.update(
					writeOutput.getInput(), updateOutput, COUNT_WRITTEN, 10, 1
				);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			if(COUNT_UPDATED > 0) {
				COUNT_READ = client.read(updateOutput.getInput(), null, COUNT_UPDATED, 10, true);
			} else {
				throw new IllegalStateException("Failed to update");
			}
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	private final static Pattern PATTERN_OBJ_METAINFO = Pattern.compile(
		"(?<id>[\\da-z]{8,64}),(?<offset>[\\da-f]+),(?<size>\\d+),(?<layer>[\\da-f]+)/(?<mask>[\\da-f]+)"
	);
	//
	@Test
	public void checkUpdatedItems()
		throws Exception {
		int layer, mask, size;
		String s;
		Matcher m;
		for(final HttpDataItem obj : BUFF_UPDATE) {
			s = obj.toString();
			m = PATTERN_OBJ_METAINFO.matcher(s);
			if(m.find()) {
				layer = Integer.parseInt(m.group("layer"), 0x10);
				if(layer != 0) {
					Assert.fail("Invalid layer value: " + s);
					break;
				}
				mask = Integer.parseInt(m.group("mask"), 0x10);
				if(mask == 0) {
					Assert.fail("Invalid mask value: " + s);
					break;
				}
				size = Integer.parseInt(m.group("size"));
				if(size != 1024) {
					Assert.fail("Invalid size value: " + s);
					break;
				}
			} else {
				Assert.fail("Invalid metainfo line: " + s);
			}
		}
	}
}
