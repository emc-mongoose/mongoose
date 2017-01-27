package com.emc.mongoose.storage.driver.nio.fs;

import static com.emc.mongoose.model.io.task.IoTask.Status;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeCount;
import static com.emc.mongoose.model.item.MutableDataItem.getRangeOffset;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.io.ThreadLocalByteBuffer;
import com.emc.mongoose.model.io.task.data.mutable.MutableDataIoTask;
import static com.emc.mongoose.model.io.task.data.DataIoTask.DataIoResult;
import com.emc.mongoose.model.item.DataItem;
import com.emc.mongoose.model.item.MutableDataItem;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;
import com.emc.mongoose.model.io.IoType;
import com.emc.mongoose.storage.driver.nio.base.NioStorageDriverBase;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Markers;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 Created by kurila on 19.07.16.
 */
public final class BasicFileStorageDriver<
	I extends MutableDataItem, O extends MutableDataIoTask<I, R>, R extends DataIoResult
>
extends NioStorageDriverBase<I, O, R>
implements FileStorageDriver<I, O, R> {

	private static final Logger LOG = LogManager.getLogger();
	private static final Set<OpenOption> CREATE_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.CREATE);
			add(StandardOpenOption.TRUNCATE_EXISTING);
			add(StandardOpenOption.WRITE);
		}
	};
	private static final Set<OpenOption> READ_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.READ);
		}
	};
	private static final Set<OpenOption> WRITE_OPEN_OPT = new HashSet<OpenOption>() {
		{
			add(StandardOpenOption.WRITE);
		}
	};

	private final Map<O, FileChannel> srcOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openSrcFileFunc;
	
	private final Map<String, File> dstParentDirs = new ConcurrentHashMap<>();
	private final Function<String, File> createParentDirFunc;
	private final Map<O, FileChannel> dstOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openDstFileFunc;
	
	public BasicFileStorageDriver(
		final String jobName, final LoadConfig loadConfig, final boolean verifyFlag
	) {
		super(jobName, loadConfig, verifyFlag);
		
		openSrcFileFunc = ioTask -> {
			final String srcPath = ioTask.getSrcPath();
			if(srcPath == null || srcPath.isEmpty()) {
				return null;
			}
			final String fileItemName = ioTask.getItem().getName();
			final Path srcFilePath = srcPath.isEmpty() || fileItemName.startsWith(srcPath) ?
				FS.getPath(fileItemName) : FS.getPath(srcPath, fileItemName);
			try {
				return FS_PROVIDER.newFileChannel(srcFilePath, READ_OPEN_OPT);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to open the source channel for the path @ \"{}\"",
					srcFilePath
				);
				return null;
			}
		};
		
		createParentDirFunc = parentPath -> {
			try {
				final File parentDir = FS.getPath(parentPath).toFile();
				if(!parentDir.exists()) {
					parentDir.mkdirs();
				}
				return parentDir;
			} catch(final Exception e) {
				return null;
			}
		};
		
		openDstFileFunc = ioTask -> {
			final String fileItemName = ioTask.getItem().getName();
			final IoType ioType = ioTask.getIoType();
			final String dstPath = ioTask.getDstPath();
			final Path itemPath;
			try {
				if(dstPath == null || dstPath.isEmpty() || fileItemName.startsWith(dstPath)) {
					itemPath = FS.getPath(fileItemName);
				} else {
					dstParentDirs.computeIfAbsent(dstPath, createParentDirFunc);
					itemPath = FS.getPath(dstPath, fileItemName);

				}
				if(IoType.CREATE.equals(ioType)) {
					return FS_PROVIDER.newFileChannel(itemPath, CREATE_OPEN_OPT);
				} else {
					return FS_PROVIDER.newFileChannel(itemPath, WRITE_OPEN_OPT);
				}
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.WARN, e, "Failed to open the output channel for the path \"{}\"",
					dstPath
				);
				return null;
			}
		};
	}
	
	@Override
	public final boolean createPath(final String path)
	throws RemoteException {
		final File pathFile = FS.getPath(path).toFile();
		if(!pathFile.exists()) {
			LOG.info(Markers.MSG, "Create the output path: \"{}\"", path);
			return pathFile.mkdirs();
		} else {
			LOG.info(Markers.MSG, "Output path \"{}\" already exists", path);
			return true;
		}
	}
	
	@Override
	public final void adjustIoBuffers(final SizeInBytes avgDataItemSize, final IoType ioType) {
	}
	
	@Override
	protected final void invokeNio(final O ioTask) {

		FileChannel srcChannel = null;
		FileChannel dstChannel = null;

		try {

			final IoType ioType = ioTask.getIoType();
			final I item = ioTask.getItem();

			switch(ioType) {

				case NOOP:
					finishIoTask(ioTask);
					break;
				
				case CREATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, openDstFileFunc);
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, openSrcFileFunc);
					if(dstChannel == null) {
						ioTask.setStatus(Status.FAIL_IO);
					} else if(srcChannel == null) {
						invokeCreate(item, ioTask, dstChannel);
					} else { // copy the data from the src channel to the dst channel
						invokeCopy(item, ioTask, srcChannel, dstChannel);
					}
					break;
				
				case READ:
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, openSrcFileFunc);
					if(srcChannel == null) {
						ioTask.setStatus(Status.FAIL_IO);
					} else if(verifyFlag) {
						try {
							invokeReadAndVerify(item, ioTask, srcChannel);
						} catch(final DataSizeException e) {
							final long countBytesDone = ioTask.getCountBytesDone() + e.getOffset();
							LOG.warn(
								Markers.MSG, "{}: content size mismatch, expected: {}, actual: {}",
								item.getName(), item.size(), countBytesDone
							);
							ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
						}
					} else {
						invokeRead(item, ioTask, srcChannel);
					}
					break;
				
				case UPDATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, openDstFileFunc);
					if(dstChannel == null) {
						ioTask.setStatus(Status.FAIL_IO);
					} else {
						final List<ByteRange> fixedByteRanges = ioTask.getFixedRanges();
						if(fixedByteRanges == null || fixedByteRanges.isEmpty()) {
							invokeRandomRangesUpdate(item, ioTask, dstChannel);
						} else {
							invokeFixedRangesUpdate(item, ioTask, dstChannel, fixedByteRanges);
						}
					}
					break;
				
				case DELETE:
					invokeDelete(ioTask);
					break;
				
				default:
					ioTask.setStatus(Status.FAIL_UNKNOWN);
					LOG.fatal(Markers.ERR, "Unknown load type \"{}\"", ioType);
					break;
			}
		} catch(final FileNotFoundException e) {
			LogUtil.exception(LOG, Level.WARN, e, ioTask.toString());
			ioTask.setStatus(Status.RESP_FAIL_NOT_FOUND);
		} catch(final AccessDeniedException e) {
			LogUtil.exception(LOG, Level.WARN, e, ioTask.toString());
			ioTask.setStatus(Status.RESP_FAIL_AUTH);
		} catch(final ClosedChannelException e) {
			ioTask.setStatus(Status.CANCELLED);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, ioTask.toString());
			ioTask.setStatus(Status.FAIL_IO);
		} catch(final NullPointerException e) {
			if(!isClosed()) { // shared content source may be already closed from the load generator
				e.printStackTrace(System.out);
				ioTask.setStatus(Status.FAIL_UNKNOWN);
			}
		} catch(final Throwable e) {
			// should be Throwable here in order to make the closing block further always reachable
			// the same effect may be reached using "finally" block after this "catch"
			e.printStackTrace(System.err);
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
	
	private void finishIoTask(final O ioTask) {
		ioTask.startResponse();
		ioTask.finishResponse();
		ioTask.setStatus(Status.SUCC);
	}

	private void invokeCreate(final I fileItem, final O ioTask, final FileChannel dstChannel)
	throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && Status.ACTIVE.equals(ioTask.getStatus())) {
			countBytesDone += fileItem.write(dstChannel, contentSize - countBytesDone);
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			finishIoTask(ioTask);
		}
	}
	
	private void invokeCopy(
		final I fileItem, final O ioTask, final FileChannel srcChannel, final FileChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && Status.ACTIVE.equals(ioTask.getStatus())) {
			countBytesDone += srcChannel.transferTo(
				countBytesDone, contentSize - countBytesDone, dstChannel
			);
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadAndVerify(final I fileItem, final O ioTask, final FileChannel srcChannel)
	throws DataSizeException, IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize) {
			try {
				if(fileItem.isUpdated()) {
					final DataItem currRange = ioTask.getCurrRange();
					final int nextRangeIdx = ioTask.getCurrRangeIdx() + 1;
					final long nextRangeOffset = getRangeOffset(nextRangeIdx);
					if(currRange != null) {
						final int n = currRange.readAndVerify(
							srcChannel, ThreadLocalByteBuffer.get(nextRangeOffset - countBytesDone)
						);
						if(n < 0) {
							throw new DataSizeException(contentSize, countBytesDone);
						} else {
							countBytesDone += n;
							if(countBytesDone == nextRangeOffset) {
								ioTask.setCurrRangeIdx(nextRangeIdx);
							}
						}
					} else {
						throw new AssertionError("Null data range");
					}
				} else {
					final int n = fileItem.readAndVerify(
						srcChannel, ThreadLocalByteBuffer.get(contentSize - countBytesDone)
					);
					if(n < 0) {
						throw new DataSizeException(contentSize, countBytesDone);
					}
					countBytesDone += n;
				}
			} catch(final DataCorruptionException e) {
				ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
				countBytesDone += e.getOffset();
				ioTask.setCountBytesDone(countBytesDone);
				LOG.warn(
					Markers.MSG, "{}: content mismatch @ offset {}, expected: {}, actual: {} ",
					fileItem.getName(), countBytesDone,
					String.format("\"0x%X\"", e.expected), String.format("\"0x%X\"", e.actual)
				);
			}
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			finishIoTask(ioTask);
		}
	}
	
	private void invokeRead(final I fileItem, final O ioTask, final FileChannel srcChannel)
	throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		int n;
		if(countBytesDone < contentSize) {
			n = srcChannel.read(ThreadLocalByteBuffer.get(contentSize - countBytesDone));
			if(n < 0) {
				finishIoTask(ioTask);
				ioTask.setCountBytesDone(countBytesDone);
				fileItem.size(countBytesDone);
			}
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeRandomRangesUpdate(
		final I fileItem, final O ioTask, final FileChannel dstChannel
	) throws IOException {
		
		long countBytesDone = ioTask.getCountBytesDone();
		final long updatingRangesSize = ioTask.getUpdatingRangesSize();
		
		if(updatingRangesSize > 0 && updatingRangesSize > countBytesDone) {
			
			DataItem updatingRange;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.getCurrRangeIdx();
				if(currRangeIdx < getRangeCount(fileItem.size())) {
					updatingRange = ioTask.getCurrRangeUpdate();
					if(updatingRange == null) {
						ioTask.setCurrRangeIdx(++ currRangeIdx);
					} else {
						break;
					}
				} else {
					ioTask.setCountBytesDone(updatingRangesSize);
					return;
				}
			}
			
			final long updatingRangeSize = updatingRange.size();
			dstChannel.position(getRangeOffset(currRangeIdx) + countBytesDone);
			countBytesDone += updatingRange.write(dstChannel, updatingRangeSize - countBytesDone);
			if(countBytesDone == updatingRangeSize) {
				ioTask.setCurrRangeIdx(currRangeIdx + 1);
				ioTask.setCountBytesDone(0);
			}
		} else {
			finishIoTask(ioTask);
			fileItem.commitUpdatedRanges(ioTask.getUpdRangesMaskPair());
		}
	}

	private void invokeFixedRangesUpdate(
		final I fileItem, final O ioTask, final FileChannel dstChannel,
		final List<ByteRange> byteRanges
	) throws IOException {

		long countBytesDone = ioTask.getCountBytesDone();
		final long baseItemSize = fileItem.size();
		final long updatingRangesSize = ioTask.getUpdatingRangesSize();

		if(updatingRangesSize > 0 && updatingRangesSize > countBytesDone) {

			ByteRange byteRange;
			DataItem updatingRange;
			int currRangeIdx = ioTask.getCurrRangeIdx();
			long rangeBeg;
			long rangeEnd;

			if(currRangeIdx < byteRanges.size()) {
				byteRange = byteRanges.get(currRangeIdx);
				rangeBeg = byteRange.getBeg();
				rangeEnd = byteRange.getEnd();
				if(rangeBeg == -1) {
					rangeBeg = baseItemSize;
					updatingRange = fileItem.slice(rangeBeg, rangeEnd);
				} else if(rangeEnd == -1) {
					updatingRange = fileItem.slice(rangeBeg, baseItemSize - rangeBeg);
				} else {
					updatingRange = fileItem.slice(rangeBeg, rangeEnd - rangeBeg + 1);
				}
				final long updatingRangeSize = updatingRange.size();

				dstChannel.position(rangeBeg + countBytesDone);
				countBytesDone += updatingRange.write(
					dstChannel, updatingRangeSize - countBytesDone
				);

				if(countBytesDone == updatingRangeSize) {
					ioTask.setCurrRangeIdx(currRangeIdx + 1);
					ioTask.setCountBytesDone(0);
				}
			} else {
				ioTask.setCountBytesDone(updatingRangesSize);
			}
		} else {
			finishIoTask(ioTask);
			fileItem.size(baseItemSize + updatingRangesSize);
		}
	}

	private void invokeDelete(final O ioTask)
	throws IOException {
		final String dstPath = ioTask.getDstPath();
		final I fileItem = ioTask.getItem();
		Files.delete(
			dstPath == null ? Paths.get(fileItem.getName()) : Paths.get(dstPath, fileItem.getName())
		);
		finishIoTask(ioTask);
	}

	@Override
	protected final void doClose()
	throws IOException {
		super.doClose();
		for(final FileChannel srcChannel : srcOpenFiles.values()) {
			if(srcChannel.isOpen()) {
				srcChannel.close();
			}
		}
		for(final FileChannel dstChannel : dstOpenFiles.values()) {
			if(dstChannel.isOpen()) {
				dstChannel.close();
			}
		}
	}
	
	@Override
	public final String toString() {
		return String.format(super.toString(), "fs");
	}
}
