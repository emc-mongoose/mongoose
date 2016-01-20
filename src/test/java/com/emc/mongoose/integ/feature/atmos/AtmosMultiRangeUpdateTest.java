package com.emc.mongoose.integ.feature.atmos;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
//
import com.emc.mongoose.core.impl.item.base.ListItemDst;
//
import com.emc.mongoose.integ.base.StandaloneClientTestBase;
//
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
 Created by kurila on 03.08.15.
 */
public final class AtmosMultiRangeUpdateTest
extends StandaloneClientTestBase {
	//
	private final static int COUNT_TO_WRITE = 1000;
	private final static List<HttpDataItem>
		BUFF_WRITE = new ArrayList<>(COUNT_TO_WRITE),
		BUFF_UPDATE0 = new ArrayList<>(COUNT_TO_WRITE),
		BUFF_UPDATE1 = new ArrayList<>(COUNT_TO_WRITE);
	//
	private static long COUNT_WRITTEN, COUNT_UPDATED0, COUNT_READ0, COUNT_UPDATED1, COUNT_READ1;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		//
		System.setProperty(
			RunTimeConfig.KEY_RUN_ID, AtmosMultiRangeUpdateTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem>
				client = CLIENT_BUILDER
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setAPI("atmos")
					.build()
		) {
			final ItemDst<HttpDataItem> writeOutput = new ListItemDst<>(BUFF_WRITE);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("10KB")
			);
			final ItemDst<HttpDataItem> updateOutput0 = new ListItemDst<>(BUFF_UPDATE0);
			if(COUNT_WRITTEN > 0) {
				COUNT_UPDATED0 = client.update(
					writeOutput.getItemSrc(), updateOutput0, COUNT_UPDATED0, 10, 10
				);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			if(COUNT_UPDATED0 > 0) {
				COUNT_READ0 = client.read(updateOutput0.getItemSrc(), null, COUNT_UPDATED0, 10, true);
			} else {
				throw new IllegalStateException("Failed to update the 1st time");
			}
			final ItemDst<HttpDataItem> updateOutput1 = new ListItemDst<>(BUFF_UPDATE0);
			COUNT_UPDATED1 = client.update(
				writeOutput.getItemSrc(), updateOutput1, COUNT_UPDATED0, 10, 10
			);
			if(COUNT_UPDATED1 > 0) {
				COUNT_READ1 = client.read(updateOutput1.getItemSrc(), null, COUNT_UPDATED1, 10, true);
			} else {
				throw new IllegalStateException("Failed to update the 2nd time");
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
	public void checkMultiplyUpdatedTwiceItems()
	throws Exception {
		int layer, mask, size;
		String s;
		Matcher m;
		for(final HttpDataItem obj : BUFF_UPDATE1) {
			s = obj.toString();
			m = PATTERN_OBJ_METAINFO.matcher(s);
			if(m.find()) {
				layer = Integer.parseInt(m.group("layer"), 0x10);
				if(layer < 1) {
					Assert.fail("Invalid layer value: " + s);
					break;
				}
				mask = Integer.parseInt(m.group("mask"), 0x10);
				if(mask == 0) {
					Assert.fail("Invalid mask value: " + s);
					break;
				}
				size = Integer.parseInt(m.group("size"));
				if(size != 10240) {
					Assert.fail("Invalid size value: " + s);
					break;
				}
			} else {
				Assert.fail("Invalid metainfo line: " + s);
			}
		}
	}
}
