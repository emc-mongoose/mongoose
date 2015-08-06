package com.emc.mongoose.integ.core.api.atmos;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.DataItemOutput;
import com.emc.mongoose.core.impl.data.model.ListItemOutput;
import com.emc.mongoose.util.client.api.StorageClient;
import com.emc.mongoose.util.client.impl.BasicWSClientBuilder;
import org.junit.AfterClass;
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
public class AtmosSingleRangeUpdateTest {
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
		try(
			final StorageClient<WSObject>
				client = new BasicWSClientBuilder<>()
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setAPI("atmos")
				.build()
		) {
			final DataItemOutput<WSObject> writeOutput = new ListItemOutput<>(BUFF_WRITE);
			COUNT_WRITTEN = client.write(
				null, writeOutput, COUNT_TO_WRITE, 10, SizeUtil.toSize("1KB")
			);
			final DataItemOutput<WSObject> updateOutput = new ListItemOutput<>(BUFF_UPDATE);
			COUNT_UPDATED = client.update(
				writeOutput.getInput(), updateOutput, COUNT_TO_WRITE, 10, 1
			);
			COUNT_READ = client.read(updateOutput.getInput(), null, COUNT_UPDATED, 10, true);
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
		throws Exception {
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
