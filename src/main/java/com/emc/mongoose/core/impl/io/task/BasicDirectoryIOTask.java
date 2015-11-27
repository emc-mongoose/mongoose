package com.emc.mongoose.core.impl.io.task;
//
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.DirectoryIOTask;
//
import com.emc.mongoose.core.impl.data.model.DirectoryItemSrc;
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
	//
	public BasicDirectoryIOTask(final C item, final X ioConfig) {
		super(item, null, ioConfig);
		//
		fPath = Paths.get(ioConfig.getNamePrefix(), item.getName());
	}
	//
	@Override
	public void run() {
		try {
			switch(ioType) {
				case CREATE:
					runWrite();
					break;
				case READ:
					runRead();
					break;
				case DELETE:
					runDelete();
					break;
				case UPDATE:
					status = Status.RESP_FAIL_CLIENT;
					break;
				case APPEND:
					status = Status.RESP_FAIL_CLIENT;
					break;
			}
		} catch(final NoSuchFileException e) {
			status = Status.RESP_FAIL_NOT_FOUND;
		} catch(final IOException e) {
			status = Status.FAIL_IO;
		} catch(final Exception e) {
			status = Status.FAIL_UNKNOWN;
			LogUtil.exception(LOG, Level.WARN, e, "Failed to create the directory \"{}\"", fPath);
		}
	}
	//
	protected void runWrite()
	throws IOException {
		Files.createDirectories(fPath);
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
		}
	}
	//
	protected void runDelete()
	throws IOException {
		Files.delete(fPath);
	}
}
