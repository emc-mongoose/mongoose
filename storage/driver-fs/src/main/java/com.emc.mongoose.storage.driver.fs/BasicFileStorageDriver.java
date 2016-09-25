package com.emc.mongoose.storage.driver.fs;

import com.emc.mongoose.model.api.io.task.DataIoTask;
import static com.emc.mongoose.model.api.io.task.IoTask.Status;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import com.emc.mongoose.model.api.item.MutableDataItem;
import com.emc.mongoose.model.api.load.StorageDriver;
import com.emc.mongoose.model.impl.data.DataCorruptionException;
import com.emc.mongoose.model.impl.data.DataSizeException;
import com.emc.mongoose.model.util.IoWorker;
import com.emc.mongoose.model.util.LoadType;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.storage.driver.base.NioStorageDriverBase;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static java.io.File.separatorChar;

/**
 Created by kurila on 19.07.16.
 */
public final class BasicFileStorageDriver<I extends MutableDataItem, O extends DataIoTask<I>>
extends NioStorageDriverBase<I, O>
implements StorageDriver<I, O> {

	private final static Logger LOG = LogManager.getLogger();

	private final Map<O, FileChannel> srcOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openSrcFileFunc;
	private final Map<O, FileChannel> dstOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openDstFileFunc;

	public BasicFileStorageDriver(
		final String runId, final AuthConfig authConfig, final LoadConfig loadConfig,
		final String srcContainer, final boolean verifyFlag, final SizeInBytes ioBuffSize
	) {
		super(runId, authConfig, loadConfig, srcContainer, verifyFlag, ioBuffSize);
		openSrcFileFunc = ioTask -> {
			final I fileItem = ioTask.getItem();
			final Path srcFilePath;
			if(srcContainer == null) {
				srcFilePath = Paths.get(fileItem.getPath() + separatorChar + fileItem.getName());
			} else {
				srcFilePath = Paths.get(
					srcContainer, fileItem.getPath() + separatorChar + fileItem.getName()
				);
			}
			try {
				return FileChannel.open(srcFilePath, StandardOpenOption.READ);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to open the source channel for the path @ \"{}\"",
					srcFilePath
				);
				return null;
			}
		};
		openDstFileFunc = ioTask -> {
			final I fileItem = ioTask.getItem();
			final LoadType ioType = ioTask.getLoadType();
			Path dstPath = Paths.get(ioTask.getDstPath());
			try {
				if(!Files.exists(dstPath)) {
					Files.createDirectories(dstPath);
				}
				dstPath = Paths.get(dstPath.toString(), fileItem.getName());
				if(LoadType.CREATE.equals(ioType)) {
					return FileChannel.open(
						dstPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
					);
				} else {
					return FileChannel.open(dstPath, StandardOpenOption.WRITE);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to open the source channel for the path \"{}\"",
					dstPath
				);
				return null;
			}
		};
	}

	@Override
	protected final void invokeNio(final O ioTask) {

		if(Status.PENDING.equals(ioTask.getStatus())) {
			// mark the task as active if it is invoked 1st time
			ioTask.startRequest();
			ioTask.finishRequest();
		}

		FileChannel srcChannel = null;
		FileChannel dstChannel = null;

		try {

			final LoadType ioType = ioTask.getLoadType();
			final I item = ioTask.getItem();

			switch(ioType) {
				case CREATE:
					if(srcContainer != null) { // copy mode
						srcChannel = srcOpenFiles.computeIfAbsent(ioTask, openSrcFileFunc);
					}
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, openDstFileFunc);
					invokeCreate(item, ioTask, srcChannel, dstChannel);
					break;
				case READ:
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, openSrcFileFunc);
					try {
						invokeRead(item, ioTask, srcChannel);
					} catch(final DataSizeException e) {
						final long countBytesDone = ioTask.getCountBytesDone() + e.offset;
						LOG.warn(
							Markers.MSG, "{}: content size mismatch, expected: {}, actual: {}",
							item.getName(), item.size(), countBytesDone
						);
						ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
					}
					break;
				case UPDATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, openDstFileFunc);
					invokeUpdate(item, ioTask, dstChannel);
					break;
				case DELETE:
					invokeDelete(item, ioTask);
					break;
				default:
					ioTask.setStatus(Status.FAIL_UNKNOWN);
					LOG.fatal(Markers.ERR, "Unknown load type \"{}\"", ioType);
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
			// should be Throwable here in order to make the closing block further always reachable
			// the same effect may be reached using "finally" block after this "catch"
			e.printStackTrace(System.out);
			ioTask.setStatus(Status.FAIL_UNKNOWN);
		}

		if(!Status.ACTIVE.equals(ioTask.getStatus())) {

			if(srcChannel != null && srcChannel.isOpen()) {
				srcOpenFiles.remove(ioTask);
				try {
					srcChannel.close();
				} catch(final IOException e) {
					LOG.warn(Markers.ERR, "Failed to close the source I/O channel");
				}
			}

			if(dstChannel != null && dstChannel.isOpen()) {
				dstOpenFiles.remove(ioTask);
				try {
					dstChannel.close();
				} catch(final IOException e) {
					LOG.warn(Markers.ERR, "Failed to close the destination I/O channel");
				}
			}
		}
	}

	private void invokeCreate(
		final I fileItem, final O ioTask, final FileChannel srcChannel, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && Status.ACTIVE.equals(ioTask.getStatus())) {
			if(srcChannel == null) {
				countBytesDone += fileItem.write(dstChannel, contentSize - countBytesDone);
				ioTask.setCountBytesDone(countBytesDone);
			} else {
				// copy mode
				countBytesDone += srcChannel.transferTo(
					countBytesDone, contentSize - countBytesDone, dstChannel
				);
				ioTask.setCountBytesDone(countBytesDone);
			}
		} else {
			ioTask.startResponse();
			ioTask.finishResponse();
			ioTask.setStatus(Status.SUCC);
		}
	}

	private void invokeRead(final I fileItem, final O ioTask, final FileChannel srcChannel)
	throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize) {
			final ByteBuffer buffIn = ((IoWorker) Thread.currentThread())
				.getThreadLocalBuff(contentSize - countBytesDone);
			if(verifyFlag) {
				int currRangeIdx = 0;
				try {
					if(fileItem.hasBeenUpdated()) {
						// TODO verify updated file items
						throw new IllegalStateException("Not implemented yet");
					} else {
						countBytesDone += fileItem.readAndVerify(srcChannel, buffIn);
					}
				} catch(final DataCorruptionException e) {
					ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
					countBytesDone += e.offset;
					ioTask.setCountBytesDone(countBytesDone);
					LOG.warn(
						Markers.MSG,
						"{}: content mismatch @ offset {}, expected: {}, actual: {} " +
						"(within byte range which is {})", fileItem.getName(), countBytesDone,
						String.format("\"0x%X\"", e.expected), String.format("\"0x%X\"", e.actual),
						fileItem.isCurrLayerRangeUpdated(currRangeIdx) ? "UPDATED" : "NOT updated"
					);
				}
			} else {
				countBytesDone += srcChannel.read(buffIn);
			}
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			ioTask.startResponse();
			ioTask.finishResponse();
			ioTask.setStatus(Status.SUCC);
		}
	}

	private void invokeUpdate(
		final I fileItem, final O ioTask, final FileChannel dstChannel
	) throws IOException {
		// TODO
		ioTask.startResponse();
		ioTask.finishResponse();
		ioTask.setStatus(Status.SUCC);
	}

	private void invokeDelete(final I fileItem, final O ioTask)
	throws IOException {
		final Path dstPath = Paths.get(ioTask.getDstPath());
		Files.delete(dstPath);
		ioTask.startResponse();
		ioTask.finishResponse();
		ioTask.setStatus(Status.SUCC);
	}
}
