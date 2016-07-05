package com.emc.mongoose.system.feature.distributed;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.impl.item.container.BasicDirectory;
import com.emc.mongoose.core.impl.item.data.ContentSourceUtil;
import com.emc.mongoose.core.impl.item.data.CsvFileDataItemInput;
import com.emc.mongoose.system.base.DistributedFileSystemTestBase;
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
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 14.07.15.
 */
public final class ReadDirsWithFilesDistributedTest
extends DistributedFileSystemTestBase {
	//
	private final static long COUNT_TO_WRITE = 100;
	private final static String RUN_ID = ReadDirsWithFilesDistributedTest.class.getCanonicalName();
	//
	private static long countWritten, countRead;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LogValidator.removeDirectory(Paths.get("/tmp/" + RUN_ID));
		LogValidator.removeLogDirectory(RUN_ID);
		LogValidator.removeLogDirectory(RUN_ID + "_FilesWrite");
		LogValidator.removeLogDirectory(RUN_ID + "_DirsRead");
		//
		System.setProperty(AppConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, "/tmp/" + RUN_ID);
		DistributedFileSystemTestBase.setUpClass();
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setStorageType("fs")
				.setItemType("container")
				.build()
		) {
			countWritten = client.create(null, COUNT_TO_WRITE, 100, 0);
			RunIdFileManager.flush(RUN_ID);
		}
		//
		final File dirListFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("directories list file doesn't exist", dirListFile.exists());
		String nextDirName;
		StorageClient<FileItem> nextDirClient;
		CLIENT_BUILDER
			.setStorageType("fs")
			.setItemType("data")
			.setLimitCount(COUNT_TO_WRITE);
		final AppConfig rtConfig = BasicConfig.THREAD_CONTEXT.get();
		rtConfig.setRunId(RUN_ID + "_FilesWrite");
		try(
			final BufferedReader
				in = Files.newBufferedReader(dirListFile.toPath(), StandardCharsets.UTF_8)
		) {
			do {
				nextDirName = in.readLine();
				if(nextDirName == null) {
					break;
				} else {
					rtConfig.setProperty(
						AppConfig.KEY_ITEM_DST_CONTAINER, "/tmp/" + RUN_ID + "/" + nextDirName
					);
					nextDirClient = CLIENT_BUILDER.build();
					nextDirClient.create(null, COUNT_TO_WRITE, 100, 1024);
				}
			} while(true);
		}
		//
		RunIdFileManager.flush(RUN_ID);
		TimeUnit.SECONDS.sleep(10);
		//
		rtConfig.setRunId(RUN_ID + "_DirsRead");
		rtConfig.setProperty(AppConfig.KEY_ITEM_DST_CONTAINER, "/tmp/" + RUN_ID);
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setStorageType("fs")
				.setItemType("container")
				.build()
		) {
			countRead = client.read(
				new CsvFileDataItemInput<FileItem>(
					dirListFile.toPath(), (Class) BasicDirectory.class,
					ContentSourceUtil.getDefaultInstance()
				),
				null, countWritten, 3, true
			);
		}
		RunIdFileManager.flush(rtConfig.getRunId());
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(AppConfig.KEY_STORAGE_TYPE, "http");
		System.setProperty(AppConfig.KEY_ITEM_TYPE, "data");
		DistributedFileSystemTestBase.tearDownClass();
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		final File tgtDirContent[] = tgtDir.listFiles();
		if(tgtDirContent != null) {
			for(final File f : tgtDir.listFiles()) {
				for(final File ff : f.listFiles()) {
					ff.delete();
				}
				f.delete();
			}
		}
		tgtDir.delete();
	}
	//
	@Test
	public void checkDestDir() {
		final File tgtDirFiles[] = Paths.get("/tmp/" + RUN_ID).toFile().listFiles();
		Assert.assertEquals(COUNT_TO_WRITE, tgtDirFiles == null ? -1 : tgtDirFiles.length);
	}
	//
	@Test
	public void checkReturnedCount() {
		Assert.assertEquals(COUNT_TO_WRITE, countWritten);
		Assert.assertEquals(COUNT_TO_WRITE, countRead);
	}
	//
	@Test
	public void checkLoggedItemsCount()
	throws Exception {
		int itemsCount = 0;
		try(
			final BufferedReader in = Files.newBufferedReader(
				LogValidator.getItemsListFile(RUN_ID + "_DirsRead").toPath(), StandardCharsets.UTF_8
			)
		) {
			while(in.readLine() != null) {
				itemsCount ++;
			}
		}
		Assert.assertEquals(
			"Expected " + countWritten + " in the output CSV file, but got " + itemsCount,
			countWritten, itemsCount
		);
	}
}
