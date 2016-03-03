package com.emc.mongoose.integ.feature.filesystem;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.impl.item.base.ListItemDst;
import com.emc.mongoose.core.impl.item.base.ListItemSrc;
import com.emc.mongoose.integ.base.FileSystemTestBase;
import com.emc.mongoose.integ.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 04.12.15.
 */
public class AppendTest
extends FileSystemTestBase {
	//
	private final static int COUNT_TO_WRITE = 10000;
	private final static String RUN_ID = AppendTest.class.getCanonicalName();
	//
	private static long countWritten, countAppended;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, "/tmp/" + RUN_ID);
		FileSystemTestBase.setUpClass();
		final List<FileItem>
			itemBuffAppend = new ArrayList<>(COUNT_TO_WRITE);
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setItemClass("file")
				.build()
		) {
			countWritten = client.write(
				null, new ListItemDst<>(itemBuffAppend), COUNT_TO_WRITE, 100,
				SizeUtil.toSize("100KB")
			);
			TimeUnit.SECONDS.sleep(1);
			countAppended = client.append(
				new ListItemSrc<>(itemBuffAppend), null, countWritten, 100,
				SizeUtil.toSize("100KB")
			);
			TimeUnit.SECONDS.sleep(1);
			//
			RunIdFileManager.flushAll();
		}
	}
	//
	@SuppressWarnings("ResultOfMethodCallIgnored")
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_ITEM_CLASS, "data");
		System.setProperty(RunTimeConfig.KEY_ITEM_NAMING_PREFIX, "");
		FileSystemTestBase.tearDownClass();
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		final File[] files = tgtDir.listFiles();
		Assert.assertNotNull(files);
		for (final File f : files) {
			f.delete();
		}
		tgtDir.delete();
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
		Assert.assertEquals(countWritten, countAppended);
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
			"Expected " + countWritten + " in the output CSV file, but got " + itemsCount,
			itemsCount, countWritten
		);
	}
	//
	@Test
	public void checkAppendSize() {
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		final long expectedSize = SizeUtil.toSize("200KB");
		final File[] files = tgtDir.listFiles();
		Assert.assertNotNull(files);
		for (final File f : files) {
			Assert.assertEquals(expectedSize, f.length());
		}

	}
}
