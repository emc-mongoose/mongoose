package com.emc.mongoose.tests.system.util;

import com.emc.mongoose.api.common.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 Created by kurila on 06.06.17.
 */
public interface DirWithManyFilesDeleter {
	
	static void deleteExternal(final String path)
	throws IOException, InterruptedException {
		final Process p = Runtime.getRuntime().exec(new String[] {"rm", "-rf", path});
		p.waitFor();
	}
	
	static void deleteConcurrently(final String path)
	throws InterruptedException {
		final File dir = new File(path);
		final File[] files = dir.listFiles();
		final int threads = ThreadUtil.getHardwareThreadCount();
		final int fileCount = files.length;
		final int fileCountPerThread = fileCount / threads;
		final int remainingFileCount = fileCount % threads;
		if(fileCountPerThread > 1) {
			final ExecutorService concurrentDeleteSvc = Executors.newFixedThreadPool(threads);
			for(int i = 0; i < threads; i ++) {
				final int startOffset = i * fileCountPerThread;
				concurrentDeleteSvc.submit(
					() -> {
						for(int j = 0; j < fileCountPerThread; j ++) {
							files[startOffset + j].delete();
						}
					}
				);
			}
			concurrentDeleteSvc.shutdown();
			if(remainingFileCount > 0) {
				for(int i = 0; i < remainingFileCount; i ++) {
					files[fileCount - i - 1].delete();
				}
			}
			concurrentDeleteSvc.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} else {
			for(final File file : files) {
				file.delete();
			}
		}
		dir.delete();
	}
}
