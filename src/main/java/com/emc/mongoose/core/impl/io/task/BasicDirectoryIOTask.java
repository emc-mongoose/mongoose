package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
//
import com.emc.mongoose.core.impl.item.data.DirectoryItemSrc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
/**
 Created by kurila on 23.11.15.
 */
public class BasicDirectoryIOTask<
	T extends FileItem, C extends Directory<T>, X extends FileIOConfig<T, C>
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
		final String parentPath = ioConfig.getNamePrefix();
		if(parentPath != null && !parentPath.isEmpty()) {
			fPath = Paths.get(ioConfig.getNamePrefix(), item.getName()).toAbsolutePath();
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
					runWrite();
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
	protected void runWrite()
	throws IOException {
		Files.createDirectories(fPath);
		status = Status.SUCC;
	}
	//
	protected void runRead()
	throws IOException {
		try(
			final DirectoryStream<Path>
				dirStream = Files.newDirectoryStream(
					fPath, DirectoryItemSrc.DEFAULT_DIRECTORY_STREAM_FILTER
				)
		) {
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(Markers.MSG, "Directory \"{}\" listing follows", fPath);
			}
			for(final Path nextFilePath : dirStream) {
				if(LOG.isTraceEnabled(Markers.MSG)) {
					LOG.trace(Markers.MSG, nextFilePath.toAbsolutePath());
				}
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
