package com.emc.mongoose.integ.feature.filesystem;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.common.log.appenders.RunIdFileManager;
//
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.impl.container.BasicDirectory;
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
import com.emc.mongoose.core.impl.data.model.DataItemCSVFileSrc;
import com.emc.mongoose.integ.base.FileSystemTestBase;
import com.emc.mongoose.integ.tools.LogValidator;
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
public final class ReadDirsWithFilesTest
extends FileSystemTestBase {
	//
	private final static long COUNT_TO_WRITE = 100;
	private final static String RUN_ID = ReadDirsWithFilesTest.class.getCanonicalName();
	//
	private static long countWritten, countRead;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID);
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID);
		FileSystemTestBase.setUpClass();
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setLimitCount(COUNT_TO_WRITE)
				.setItemClass("directory")
				.build()
		) {
			countWritten = client.write(null, null, COUNT_TO_WRITE, 100, 0);
			//
			RunIdFileManager.flushAll();
		}
		//
		final File dirListFile = LogValidator.getItemsListFile(RUN_ID);
		Assert.assertTrue("directories list file doesn't exist", dirListFile.exists());
		String nextDirName;
		StorageClient<FileItem> nextDirClient;
		CLIENT_BUILDER
			.setItemClass("file")
			.setLimitCount(COUNT_TO_WRITE);
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		rtConfig.setProperty(RunTimeConfig.KEY_RUN_ID, RUN_ID + "_FilesWrite");
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
						RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID + "/" + nextDirName
					);
					nextDirClient = CLIENT_BUILDER.build();
					nextDirClient.write(null, null, COUNT_TO_WRITE, 100, 10);
				}
			} while(true);
		}
		//
		TimeUnit.SECONDS.sleep(1);
		//
		rtConfig.set(RunTimeConfig.KEY_RUN_ID, RUN_ID + "_DirsRead");
		rtConfig.set(RunTimeConfig.KEY_ITEM_PREFIX, "/tmp/" + RUN_ID);
		try(
			final StorageClient<FileItem> client = CLIENT_BUILDER
				.setLimitTime(0, TimeUnit.SECONDS)
				.setItemClass("directory")
				.build()
		) {
			countRead = client.read(
				new DataItemCSVFileSrc<FileItem>(
					dirListFile.toPath(), (Class) BasicDirectory.class,
					ContentSourceBase.getDefault()
				),
				null, countWritten, 100, true
			);
		}
		//
		TimeUnit.SECONDS.sleep(1);
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setProperty(RunTimeConfig.KEY_ITEM_CLASS, "data");
		System.setProperty(RunTimeConfig.KEY_ITEM_PREFIX, "");
		FileSystemTestBase.tearDownClass();
		final File tgtDir = Paths.get("/tmp/" + RUN_ID).toFile();
		for(final File f : tgtDir.listFiles()) {
			for(final File ff : f.listFiles()) {
				ff.delete();
			}
			f.delete();
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
