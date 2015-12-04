package com.emc.mongoose.integ.feature.filesystem;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.impl.data.model.ListItemDst;
import com.emc.mongoose.core.impl.data.model.ListItemSrc;
import com.emc.mongoose.integ.base.FileSystemTestBase;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.12.15.
 */
public class UpdateAndVerifyTest
extends FileSystemTestBase {
	//
	private final static int COUNT_TO_WRITE = 1000;
	private final static String RUN_ID = UpdateAndVerifyTest.class.getCanonicalName();
	//
	private static long countWritten, countUpdated, countRead;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID);
		FileSystemTestBase.setUpClass();
		final List<FileItem>
			itemBuffWritten = new ArrayList<>(COUNT_TO_WRITE),
			itemBuffUpdated = new ArrayList<>(COUNT_TO_WRITE);
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setItemClass("file")
				.build()
		) {
			countWritten = client.write(
				null, new ListItemDst<>(itemBuffWritten), COUNT_TO_WRITE, 10, SizeUtil.toSize("1MB")
			);
			TimeUnit.SECONDS.sleep(1);
			countUpdated = client.update(
				new ListItemSrc<>(itemBuffWritten), new ListItemDst<>(itemBuffUpdated),
				countWritten, 10, 10
			);
			TimeUnit.SECONDS.sleep(1);
			countRead = client.read(
				new ListItemSrc<>(itemBuffUpdated), null, countUpdated, 10, true
			);
			TimeUnit.SECONDS.sleep(1);
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "");
		FileSystemTestBase.tearDownClass();
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		for(final File f : tgtDir.listFiles()) {
			f.delete();
		}
		tgtDir.delete();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
		Assert.assertEquals(countWritten, countRead);
		Assert.assertEquals(countRead, countUpdated);
	}
	//
	@Test
	public void checkLoggedItemsCount()
	throws Exception {
		int itemsCount = 0;
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getItemsListFile(RUN_ID).toPath(), StandardCharsets.UTF_8
			)
		) {
			while(in.readLine() != null) {
				itemsCount ++;
			}
		}
		Assert.assertEquals(
			"Expected " + countRead + " in the output CSV file, but got " + itemsCount,
			itemsCount, countRead
		);
	}
	//
	@Test
	public void checkNoReadFailures() {
		try(
			final CSVParser csvParser = new CSVParser(
				Files.newBufferedReader(
					LogValidator.getPerfTraceFile(RUN_ID).toPath(), StandardCharsets.UTF_8
				),
				CSVFormat.RFC4180
			)
		) {
			String v;
			boolean firstRow = true;
			for(final CSVRecord csvRec : csvParser) {
				v = csvRec.get(4);
				if(firstRow) {
					firstRow = false;
					continue;
				}
				Assert.assertEquals(0, Integer.valueOf(v).intValue());
			}
		} catch(final Exception e) {
			Assert.fail(e.toString());
		}
	}
}
