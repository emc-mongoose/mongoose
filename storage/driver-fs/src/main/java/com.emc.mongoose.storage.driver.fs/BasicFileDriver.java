package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import static com.emc.mongoose.model.api.io.task.IoTask.Status;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.Driver;
import com.emc.mongoose.model.util.IoWorker;
import com.emc.mongoose.model.util.LoadType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

import static com.emc.mongoose.ui.config.Config.IoConfig.BufferConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;

/**
 Created by kurila on 19.07.16.
 */
public class BasicFileDriver<I extends MutableDataItem, O extends DataIoTask<I>>
extends FsDriverBase<I, O>
implements Driver<I, O> {

	private final static ThreadLocal<Map<DataIoTask, FileChannel>>
		SRC_OPEN_FILES = new ThreadLocal<Map<DataIoTask, FileChannel>>() {
			@Override
			protected final Map<DataIoTask, FileChannel> initialValue() {
				return new HashMap<>();
			}
		};
	private final static ThreadLocal<Map<DataIoTask, FileChannel>>
		DST_OPEN_FILES = new ThreadLocal<Map<DataIoTask, FileChannel>>() {
			@Override
			protected final Map<DataIoTask, FileChannel> initialValue() {
				return new HashMap<>();
			}
		};

	public BasicFileDriver(final LoadConfig loadConfig, final BufferConfig ioBufferConfig) {
		super(loadConfig, ioBufferConfig);
	}

	private FileChannel getSrcChannel(final I fileItem, final O ioTask)
	throws IOException {
		final Path srcPath = Paths.get(fileItem.getPath());
		final Map<DataIoTask, FileChannel> srcOpenFiles = SRC_OPEN_FILES.get();
		FileChannel srcChannel = srcOpenFiles.get(ioTask);
		if(srcChannel == null) {
			srcChannel  = FileChannel.open(srcPath, StandardOpenOption.READ);
			srcOpenFiles.put(ioTask, srcChannel);
		}
		return srcChannel;
	}

	private FileChannel getDstChannel(final O ioTask, final LoadType ioType)
	throws IOException {
		final Path dstPath = Paths.get(ioTask.getDstPath());
		if(!Files.exists(dstPath)) {
			Files.createDirectories(dstPath);
		}
		final Map<DataIoTask, FileChannel> dstOpenFiles = DST_OPEN_FILES.get();
		FileChannel dstChannel = dstOpenFiles.get(ioTask);
		if(dstChannel == null) {
			if(LoadType.CREATE.equals(ioType)) {
				dstChannel = FileChannel.open(
					dstPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
				);
			} else {
				dstChannel = FileChannel.open(dstPath, StandardOpenOption.WRITE);
			}
			dstOpenFiles.put(ioTask, dstChannel);
		}
		return dstChannel;
	}

	@Override
	protected void executeIoTask(final O ioTask) {

		try {
			final LoadType ioType = ioTask.getLoadType();
			final I fileItem = ioTask.getItem();
			switch(ioType) {
				case CREATE:
					createFile(
						fileItem, ioTask, getSrcChannel(fileItem, ioTask),
						getDstChannel(ioTask, ioType)
					);
					break;
				case READ:
					readFile(fileItem, ioTask, getSrcChannel(fileItem, ioTask));
					break;
				case UPDATE:
					updateFile(fileItem, ioTask, getDstChannel(ioTask, ioType));
					break;
				case DELETE:
					deleteFile(fileItem, ioTask);
					break;
			}
		} catch(final FileNotFoundException e) {
			ioTask.setStatus(Status.RESP_FAIL_NOT_FOUND);
		} catch(final AccessDeniedException e) {
			ioTask.setStatus(Status.RESP_FAIL_AUTH);
		} catch(final ClosedChannelException e) {
			ioTask.setStatus(Status.CANCELLED);
		} catch(final IOException e) {
			ioTask.setStatus(Status.FAIL_IO);
		} catch(final Throwable e) {
			ioTask.setStatus(Status.FAIL_UNKNOWN);
		}
	}

	private void createFile(
		final I fileItem, final O ioTask, final FileChannel srcChannel, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.getSize();
		if(countBytesDone < contentSize) {
			if(srcChannel == null) {
				countBytesDone += dstChannel.transferFrom(
					fileItem, countBytesDone, contentSize - countBytesDone
				);
				ioTask.setCountBytesDone(countBytesDone);
			} else {
				countBytesDone += srcChannel.transferTo(
					countBytesDone, contentSize - countBytesDone, dstChannel
				);
				ioTask.setCountBytesDone(countBytesDone);
			}
		} else {
			ioTask.setStatus(Status.SUCC);
		}
	}

	private void readFile(final I fileItem, final O ioTask, final FileChannel srcChannel)
	throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.getSize();
		if(countBytesDone < contentSize) {
			final ByteBuffer buffIn = ((IoWorker) Thread.currentThread())
				.getThreadLocalBuff(contentSize - countBytesDone);
			countBytesDone += srcChannel.read(buffIn);
			// TODO verification
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			ioTask.setStatus(Status.SUCC);
		}
	}

	private void updateFile(final I fileItem, final O ioTask, final FileChannel dstChannel)
	throws IOException {

	}

	private void deleteFile(final I fileItem, final O ioTask)
	throws IOException {
		final Path dstPath = Paths.get(ioTask.getDstPath());
		Files.delete(dstPath);
	}
}
