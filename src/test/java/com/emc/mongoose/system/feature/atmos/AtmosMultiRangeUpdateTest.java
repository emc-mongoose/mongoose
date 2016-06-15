package com.emc.mongoose.system.feature.atmos;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
//
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
//
//
import com.emc.mongoose.core.impl.item.base.ListItemOutput;
import com.emc.mongoose.system.base.StandaloneClientTestBase;
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
			AppConfig.KEY_RUN_ID, AtmosMultiRangeUpdateTest.class.getCanonicalName()
		);
		StandaloneClientTestBase.setUpClass();
		//
		try(
			final StorageClient<HttpDataItem>
				client = CLIENT_BUILDER
					.setLimitTime(0, TimeUnit.SECONDS)
					.setLimitCount(COUNT_TO_WRITE)
					.setAPI("atmos")
					.setAuth("wuser1@sanity.local", null)
					.build()
		) {
			final Output<HttpDataItem> writeOutput = new ListItemOutput<>(BUFF_WRITE);
			COUNT_WRITTEN = client.create(
				writeOutput, COUNT_TO_WRITE, 10, SizeInBytes.toFixedSize("10KB")
			);
			final Output<HttpDataItem> updateOutput0 = new ListItemOutput<>(BUFF_UPDATE0);
			if(COUNT_WRITTEN > 0) {
				COUNT_UPDATED0 = client.update(
					writeOutput.getInput(), updateOutput0, COUNT_UPDATED0, 10, 10
				);
			} else {
				throw new IllegalStateException("Failed to write");
			}
			if(COUNT_UPDATED0 > 0) {
				COUNT_READ0 = client.read(updateOutput0.getInput(), null, COUNT_UPDATED0, 10, true);
			} else {
				throw new IllegalStateException("Failed to update the 1st time");
			}
			final Output<HttpDataItem> updateOutput1 = new ListItemOutput<>(BUFF_UPDATE0);
			COUNT_UPDATED1 = client.update(
				writeOutput.getInput(), updateOutput1, COUNT_UPDATED0, 10, 10
			);
			if(COUNT_UPDATED1 > 0) {
				COUNT_READ1 = client.read(updateOutput1.getInput(), null, COUNT_UPDATED1, 10, true);
			} else {
				throw new IllegalStateException("Failed to update the 2nd time");
			}
			//
			RunIdFileManager.flush(AtmosMultiRangeUpdateTest.class.getCanonicalName());
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
