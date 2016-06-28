package com.emc.mongoose.system.feature.filesystem;
//
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.impl.item.base.ListItemOutput;
import com.emc.mongoose.core.impl.item.base.ListItemInput;
import com.emc.mongoose.system.base.FileSystemTestBase;
import com.emc.mongoose.system.tools.LogValidator;
import com.emc.mongoose.util.client.api.StorageClient;
//
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
//
import java.io.BufferedReader;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class OverwriteCircularlyTest
extends FileSystemTestBase {
	//
	private final static int COUNT_TO_WRITE = 1000, COUNT_TO_OVERWRITE = 1000000;
	private final static String RUN_ID = OverwriteCircularlyTest.class.getCanonicalName();
	//
	private static long countWritten, countOverwritten;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, "/tmp/" + RUN_ID);
		System.setProperty(AppConfig.KEY_LOAD_CIRCULAR, "true");
		FileSystemTestBase.setUpClass();
		final List<FileItem> itemBuff = new ArrayList<>(COUNT_TO_WRITE);
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setStorageType("fs")
				.build()
		) {
			countWritten = client.create(
				new ListItemOutput<>(itemBuff), COUNT_TO_WRITE, 100, 4096
			);
			countOverwritten = client.write(
				new ListItemInput<>(itemBuff), null, COUNT_TO_OVERWRITE, 100
			);
			TimeUnit.SECONDS.sleep(1);
			RunIdFileManager.flushAll();
		}
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		FileSystemTestBase.tearDownClass();
		System.setProperty(AppConfig.KEY_ITEM_TYPE, "data");
		System.setProperty(AppConfig.KEY_LOAD_CIRCULAR, "false");
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
		Assert.assertEquals(COUNT_TO_OVERWRITE, countOverwritten);
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
}
