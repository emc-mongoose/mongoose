package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
//
import com.emc.mongoose.core.impl.item.data.DirectoryItemInput;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.File;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicDirectoryIOTask<
	T extends FileItem, C extends Directory<T>, X extends FileIoConfig<T, C>
> extends BasicIOTask<C, C, X>
implements DirectoryIOTask<T, C> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Path fPath;
	private final RunnableFuture<BasicDirectoryIOTask<T, C, X>> future;
	//
	public BasicDirectoryIOTask(final C item, final X ioConfig) {
		super(item, null, ioConfig);
		//
		final C parentDir = ioConfig.getContainer();
		if(parentDir != null) {
			fPath = Paths.get(parentDir.getName(), item.getName()).toAbsolutePath();
		} else {
			fPath = Paths.get(item.getName()).toAbsolutePath();
		}
		//
		future = new FutureTask<>(this, this);
	}
	//
	@Override
	public void run() {
		reqTimeStart = reqTimeDone = respTimeStart = System.nanoTime() / 1000;
		try {
			switch(ioType) {
				case WRITE:
					runWrite(ioConfig.getCopySrcItem());
					break;
				case READ:
					runRead();
					break;
				case DELETE:
					runDelete();
					break;
			}
		} catch(final NoSuchFileException e) {
			status = Status.RESP_FAIL_NOT_FOUND;
		} catch(final IOException e) {
			status = Status.FAIL_IO;
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to {} the directory \"{}\"",
				ioType.toString().toLowerCase(), fPath
			);
		} catch(final Exception e) {
			status = Status.FAIL_UNKNOWN;
			LogUtil.exception(
				LOG, Level.WARN, e, "Failed to {} the directory \"{}\"",
				ioType.toString().toLowerCase(), fPath
			);
		} finally {
			respTimeDone = System.nanoTime() / 1000;
		}
	}
	//
	protected void runWrite(final Item copySrcDir)
	throws IOException {
		if(copySrcDir == null) {
			Files.createDirectories(fPath);
		} else {
			final C parentDir = ioConfig.getContainer();
			final Path srcPath;
			if(parentDir != null) {
				srcPath = Paths.get(parentDir.getName(), copySrcDir.getName()).toAbsolutePath();
			} else {
				srcPath = Paths.get(copySrcDir.getName()).toAbsolutePath();
			}
			copyDirWithFiles(srcPath.toFile(), fPath.toFile());
		}
		status = Status.SUCC;
	}
	//
	private void copyDirWithFiles(final File src, final File dst)
	throws IOException {
		if(src.isDirectory()) {
			copyDir(src, dst);
		} else {
			copyFile(src, dst);
		}
	}
	//
	private void copyDir(final File src, final File dst)
	throws IOException {
		if(!dst.exists()) {
			dst.mkdir();
		}
		for(final String f : src.list()) {
			copyDirWithFiles(new File(src, f), new File(dst, f));
		}
	}
	//
	private void copyFile(final File src, final File dst)
	throws IOException {
		try(
			final FileChannel srcChannel = FileChannel.open(src.toPath(), StandardOpenOption.READ)
		) {
			final long srcFileSize = srcChannel.size();
			long fileDoneByteCount = 0;
			try(
				final FileChannel dstChannel = FileChannel.open(
					dst.toPath(), StandardOpenOption.WRITE, StandardOpenOption.CREATE,
					StandardOpenOption.TRUNCATE_EXISTING
				)
			) {
				while(fileDoneByteCount < srcFileSize) {
					fileDoneByteCount += srcChannel.transferTo(0, srcChannel.size(), dstChannel);
				}
			} finally {
				countBytesDone += fileDoneByteCount;
			}
		}
	}
	//
	protected void runRead()
	throws IOException {
		try(
			final DirectoryStream<Path>
				dirStream = Files.newDirectoryStream(
					fPath, DirectoryItemInput.DEFAULT_DIRECTORY_STREAM_FILTER
				)
		) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Directory \"{}\" listing follows", fPath);
			}
			for(final Path nextFilePath : dirStream) {
				countBytesDone += Files.size(nextFilePath);
			}
			status = Status.SUCC;
		}
	}
	//
	protected void runDelete()
	throws IOException {
		Files.delete(fPath);
		status = Status.SUCC;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean cancel(final boolean mayInterruptIfRunning) {
		return future.cancel(mayInterruptIfRunning);
	}
	//
	@Override
	public final boolean isCancelled() {
		return future.isCancelled();
	}
	//
	@Override
	public final boolean isDone() {
		return future.isDone();
	}
	//
	@Override
	public final DirectoryIOTask<T, C> get()
	throws InterruptedException, ExecutionException {
		return future.get();
	}
	//
	@Override
	public final DirectoryIOTask<T, C> get(final long timeout, final TimeUnit unit)
	throws InterruptedException, ExecutionException, TimeoutException {
		return future.get(timeout, unit);
	}
}
