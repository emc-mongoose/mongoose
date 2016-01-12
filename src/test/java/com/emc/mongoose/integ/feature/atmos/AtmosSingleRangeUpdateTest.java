package com.emc.mongoose.integ.feature.atmos;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.WSObject;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 03.08.15.
 */
public class AtmosSingleRangeUpdateTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 10000;
	private final static List<WSObject>
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
			RunTimeConfig.KEY_RUN_ID, AtmosSingleRangeUpdateTest.class.getCanonicalName()
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
			final ItemDst<WSObject> writeOutput = new ListItemDst<>(BUFF_WRITE);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("1KB")
			);
			final ItemDst<WSObject> updateOutput = new ListItemDst<>(BUFF_UPDATE);
			if(COUNT_WRITTEN > 0) {
				COUNT_UPDATED = client.update(
					writeOutput.getItemSrc(), updateOutput, COUNT_WRITTEN, 10, 1
				);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			if(COUNT_UPDATED > 0) {
				COUNT_READ = client.read(updateOutput.getItemSrc(), null, COUNT_UPDATED, 10, true);
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
		for(final WSObject obj : BUFF_UPDATE) {
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
