package com.emc.mongoose.storage.driver.coop.nio.fs;

import com.emc.mongoose.data.DataCorruptionException;
import com.emc.mongoose.data.DataInput;
import com.emc.mongoose.data.DataSizeException;
import com.emc.mongoose.exception.OmgShootMyFootException;
import com.emc.mongoose.item.DataItem;
import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.io.IoType;
import com.emc.mongoose.item.io.task.IoTask;
import com.emc.mongoose.item.io.task.data.DataIoTask;
import com.emc.mongoose.item.io.task.path.PathIoTask;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.Credential;
import com.emc.mongoose.storage.driver.coop.nio.NioStorageDriver;
import com.emc.mongoose.storage.driver.coop.nio.NioStorageDriverBase;

import com.github.akurilov.commons.collection.Range;
import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 Created by kurila on 19.07.16.
 */
public final class FileStorageDriver<I extends Item, O extends IoTask<I>>
extends NioStorageDriverBase<I, O>
implements NioStorageDriver<I, O> {
	
	private final Map<DataIoTask, FileChannel> srcOpenFiles = new ConcurrentHashMap<>();
	private final Map<String, File> dstParentDirs = new ConcurrentHashMap<>();
	private final Map<DataIoTask, FileChannel> dstOpenFiles = new ConcurrentHashMap<>();

	public FileStorageDriver(
		final String stepId, final DataInput dataInput, final Config loadConfig,
		final Config storageConfig, final boolean verifyFlag
	) throws OmgShootMyFootException {
		super(stepId, dataInput, loadConfig, storageConfig, verifyFlag);
		requestAuthTokenFunc = null; // do not use
	}

	private <F extends DataItem, D extends DataIoTask<F>> FileChannel openDstFile(final D ioTask) {
		final String fileItemName = ioTask.item().getName();
		final IoType ioType = ioTask.ioType();
		final String dstPath = ioTask.dstPath();
		final Path itemPath;
		try {
			if(dstPath == null || dstPath.isEmpty() || fileItemName.startsWith(dstPath)) {
				itemPath = FsConstants.FS.getPath(fileItemName);
			} else {
				dstParentDirs.computeIfAbsent(dstPath, DirIoHelper::createParentDir);
				itemPath = FsConstants.FS.getPath(dstPath, fileItemName);
			}
			if(IoType.CREATE.equals(ioType)) {
				return FsConstants.FS_PROVIDER.newFileChannel(itemPath, FsConstants.CREATE_OPEN_OPT);
			} else {
				return FsConstants.FS_PROVIDER.newFileChannel(itemPath, FsConstants.WRITE_OPEN_OPT);
			}
		} catch(final AccessDeniedException e) {
			ioTask.status(IoTask.Status.RESP_FAIL_AUTH);
			LogUtil.exception(
				Level.DEBUG, e, "Access denied to open the output channel for the path \"{}\"",
				dstPath
			);
		} catch(final NoSuchFileException e) {
			ioTask.status(IoTask.Status.FAIL_IO);
			LogUtil.exception(
				Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"", dstPath
			);
		} catch(final FileSystemException e) {
			final long freeSpace = (new File(e.getFile())).getFreeSpace();
			if(freeSpace > 0) {
				ioTask.status(IoTask.Status.FAIL_IO);
				LogUtil.exception(
					Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"",
					dstPath
				);
			} else {
				ioTask.status(IoTask.Status.RESP_FAIL_SPACE);
				LogUtil.exception(Level.DEBUG, e, "No free space for the path \"{}\"", dstPath);
			}
		} catch(final IOException e) {
			ioTask.status(IoTask.Status.FAIL_IO);
			LogUtil.exception(
				Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"", dstPath
			);
		} catch(final Throwable cause) {
			ioTask.status(IoTask.Status.FAIL_UNKNOWN);
			LogUtil.exception(
				Level.WARN, cause, "Failed to open the output channel for the path \"{}\"",
				dstPath
			);
		}
		return null;
	}
	
	@Override
	protected final String requestNewPath(final String path) {
		final File pathFile = FsConstants.FS.getPath(path).toFile();
		if(!pathFile.exists()) {
			pathFile.mkdirs();
		}
		return path;
	}
	
	@Override
	protected final String requestNewAuthToken(final Credential credential) {
		throw new AssertionError("Should not be invoked");
	}

	@Override
	public List<I> list(
		final ItemFactory<I> itemFactory, final String path, final String prefix, final int idRadix,
		final I lastPrevItem, final int count
	) throws IOException {
		return ListingHelper.list(itemFactory, path, prefix, idRadix, lastPrevItem, count);
	}

	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final IoType ioType) {
	}

	@Override
	protected final void invokeNio(final O ioTask) {
		if(ioTask instanceof DataIoTask) {
			invokeFileNio((DataIoTask<? extends DataItem>) ioTask);
		} else if(ioTask instanceof PathIoTask) {
			throw new AssertionError("Not implemented");
		} else {
			throw new AssertionError("Not implemented");
		}
	}
	
	protected final <F extends DataItem, D extends DataIoTask<F>> void invokeFileNio(
		final D ioTask
	) {

		FileChannel srcChannel = null;
		FileChannel dstChannel = null;

		try {

			final IoType ioType = ioTask.ioType();
			final F item = ioTask.item();

			switch(ioType) {

				case NOOP:
					finishIoTask((O) ioTask);
					break;
				
				case CREATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, this::openDstFile);
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, FileIoHelper::openSrcFile);
					if(dstChannel == null) {
						break;
					}
					if(srcChannel == null) {
						if(ioTask.status().equals(IoTask.Status.FAIL_IO)) {
							break;
						} else {
							if(FileIoHelper.invokeCreate(item, ioTask, dstChannel)) {
								finishIoTask((O) ioTask);
							}
						}
					} else { // copy the data from the src channel to the dst channel
						if(FileIoHelper.invokeCopy(item, ioTask, srcChannel, dstChannel)) {
							finishIoTask((O) ioTask);
						}
					}
					break;
				
				case READ:
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, FileIoHelper::openSrcFile);
					if(srcChannel == null) {
						break;
					}
					final List<Range> fixedRangesToRead = ioTask.fixedRanges();
					if(verifyFlag) {
						try {
							if(fixedRangesToRead == null || fixedRangesToRead.isEmpty()) {
								if(ioTask.hasMarkedRanges()) {
									if(
										FileIoHelper.invokeReadAndVerifyRandomRanges(
											item, ioTask, srcChannel,
											ioTask.markedRangesMaskPair()
										)
									) {
										finishIoTask((O) ioTask);
									}
								} else {
									if(FileIoHelper.invokeReadAndVerify(item, ioTask, srcChannel)) {
										finishIoTask((O) ioTask);
									}
								}
							} else {
								if(
									FileIoHelper.invokeReadAndVerifyFixedRanges(
										item, ioTask, srcChannel, fixedRangesToRead
									)
								) {
									finishIoTask((O) ioTask);
								};
							}
						} catch(final DataSizeException e) {
							ioTask.status(IoTask.Status.RESP_FAIL_CORRUPT);
							final long
								countBytesDone = ioTask.countBytesDone() + e.getOffset();
							ioTask.countBytesDone(countBytesDone);
							Loggers.MSG.debug(
								"{}: content size mismatch, expected: {}, actual: {}",
								item.getName(), item.size(), countBytesDone
							);
						} catch(final DataCorruptionException e) {
							ioTask.status(IoTask.Status.RESP_FAIL_CORRUPT);
							final long countBytesDone = ioTask.countBytesDone() + e.getOffset();
							ioTask.countBytesDone(countBytesDone);
							Loggers.MSG.debug(
								"{}: content mismatch @ offset {}, expected: {}, actual: {} ",
								item.getName(), countBytesDone,
								String.format("\"0x%X\"", (int) (e.expected & 0xFF)),
								String.format("\"0x%X\"", (int) (e.actual & 0xFF))
							);
						}
					} else {
						if(fixedRangesToRead == null || fixedRangesToRead.isEmpty()) {
							if(ioTask.hasMarkedRanges()) {
								if(
									FileIoHelper.invokeReadRandomRanges(
										item, ioTask, srcChannel, ioTask.markedRangesMaskPair()
									)
								) {
									finishIoTask((O) ioTask);
								}
							} else {
								if(FileIoHelper.invokeRead(item, ioTask, srcChannel)) {
									finishIoTask((O) ioTask);
								}
							}
						} else {
							if(
								FileIoHelper.invokeReadFixedRanges(
									item, ioTask, srcChannel, fixedRangesToRead
								)
							) {
								finishIoTask((O) ioTask);
							}
						}
					}
					break;
				
				case UPDATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, this::openDstFile);
					if(dstChannel == null) {
						break;
					}
					final List<Range> fixedRangesToUpdate = ioTask.fixedRanges();
					if(fixedRangesToUpdate == null || fixedRangesToUpdate.isEmpty()) {
						if(ioTask.hasMarkedRanges()) {
							if(FileIoHelper.invokeRandomRangesUpdate(item, ioTask, dstChannel)) {
								item.commitUpdatedRanges(ioTask.markedRangesMaskPair());
								finishIoTask((O) ioTask);
							}
						} else {
							if(FileIoHelper.invokeOverwrite(item, ioTask, dstChannel)) {
								finishIoTask((O) ioTask);
							}
						}
					} else {
						if(
							FileIoHelper.invokeFixedRangesUpdate(
								item, ioTask, dstChannel, fixedRangesToUpdate
							)
						) {
							finishIoTask((O) ioTask);
						}
					}
					break;
				
				case DELETE:
					if(invokeDelete((O) ioTask)) {
						finishIoTask((O) ioTask);
					}
					break;
				
				default:
					ioTask.status(IoTask.Status.FAIL_UNKNOWN);
					Loggers.ERR.fatal("Unknown load type \"{}\"", ioType);
					break;
			}
		} catch(final FileNotFoundException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
			ioTask.status(IoTask.Status.RESP_FAIL_NOT_FOUND);
		} catch(final AccessDeniedException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
			ioTask.status(IoTask.Status.RESP_FAIL_AUTH);
		} catch(final ClosedChannelException e) {
			ioTask.status(IoTask.Status.INTERRUPTED);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
			ioTask.status(IoTask.Status.FAIL_IO);
		} catch(final NullPointerException e) {
			if(!isClosed()) { // shared content source may be already closed from the load generator
				e.printStackTrace(System.out);
				ioTask.status(IoTask.Status.FAIL_UNKNOWN);
			} else {
				Loggers.ERR.debug("I/O task caused NPE while being interrupted: {}", ioTask);
			}
		} catch(final Throwable e) {
			// should be Throwable here in order to make the closing block further always reachable
			// the same effect may be reached using "finally" block after this "catch"
			e.printStackTrace(System.err);
			ioTask.status(IoTask.Status.FAIL_UNKNOWN);
		}

		if(!IoTask.Status.ACTIVE.equals(ioTask.status())) {

			if(srcChannel != null) {
				srcOpenFiles.remove(ioTask);
				if(srcChannel.isOpen()) {
					try {
						srcChannel.close();
					} catch(final IOException e) {
						Loggers.ERR.warn("Failed to close the source I/O channel");
					}
				}
			}

			if(dstChannel != null) {
				dstOpenFiles.remove(ioTask);
				if(dstChannel.isOpen()) {
					try {
						dstChannel.close();
					} catch(final IOException e) {
						Loggers.ERR.warn("Failed to close the destination I/O channel");
					}
				}
			}
		}
	}

	private boolean invokeDelete(final O ioTask)
	throws IOException {
		final String dstPath = ioTask.dstPath();
		final I fileItem = ioTask.item();
		FsConstants.FS_PROVIDER.delete(
			dstPath == null ? Paths.get(fileItem.getName()) : Paths.get(dstPath, fileItem.getName())
		);
		return true;
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
		srcOpenFiles.clear();
		for(final FileChannel dstChannel : dstOpenFiles.values()) {
			if(dstChannel.isOpen()) {
				dstChannel.close();
			}
		}
		dstOpenFiles.clear();
	}
	
	@Override
	public final String toString() {
		return String.format(super.toString(), "fs");
	}
}
