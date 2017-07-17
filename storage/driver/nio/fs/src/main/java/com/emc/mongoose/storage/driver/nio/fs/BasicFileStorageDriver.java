package com.emc.mongoose.storage.driver.nio.fs;

import static com.emc.mongoose.api.model.io.task.IoTask.Status;
import static com.emc.mongoose.api.model.item.DataItem.getRangeCount;
import static com.emc.mongoose.api.model.item.DataItem.getRangeOffset;
import com.emc.mongoose.api.common.ByteRange;
import com.emc.mongoose.api.common.exception.UserShootHisFootException;
import com.emc.mongoose.api.common.io.ThreadLocalByteBuffer;
import com.emc.mongoose.api.model.data.ContentSource;
import com.emc.mongoose.api.model.io.task.data.DataIoTask;
import com.emc.mongoose.api.model.item.DataItem;
import com.emc.mongoose.api.model.data.DataCorruptionException;
import com.emc.mongoose.api.model.data.DataSizeException;
import com.emc.mongoose.api.model.io.IoType;
import com.emc.mongoose.api.model.storage.Credential;
import com.emc.mongoose.storage.driver.nio.base.NioStorageDriverBase;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.log.LogUtil;
import com.emc.mongoose.ui.log.Loggers;

import org.apache.logging.log4j.Level;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 Created by kurila on 19.07.16.
 */
public final class BasicFileStorageDriver<I extends DataItem, O extends DataIoTask<I>>
extends NioStorageDriverBase<I, O>
implements FileStorageDriver<I, O> {
	
	private final Map<O, FileChannel> srcOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openSrcFileFunc;
	
	private final Map<String, File> dstParentDirs = new ConcurrentHashMap<>();
	private final Function<String, File> createParentDirFunc;
	private final Map<O, FileChannel> dstOpenFiles = new ConcurrentHashMap<>();
	private final Function<O, FileChannel> openDstFileFunc;
	
	public BasicFileStorageDriver(
		final String jobName, final ContentSource contentSrc, final LoadConfig loadConfig,
		final StorageConfig storageConfig, final boolean verifyFlag
	) throws UserShootHisFootException {
		super(jobName, contentSrc, loadConfig, storageConfig, verifyFlag);
		
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
					Level.WARN, e, "Failed to open the source channel for the path @ \"{}\"",
					srcFilePath
				);
				ioTask.setStatus(Status.FAIL_IO);
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
			} catch(final AccessDeniedException e) {
				ioTask.setStatus(Status.RESP_FAIL_AUTH);
				LogUtil.exception(
					Level.DEBUG, e, "Access denied to open the output channel for the path \"{}\"",
					dstPath
				);
				return null;
			} catch(final NoSuchFileException e) {
				ioTask.setStatus(Status.FAIL_IO);
				LogUtil.exception(
					Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"", dstPath
				);
				return null;
			} catch(final FileSystemException e) {
				final long freeSpace = (new File(e.getFile())).getFreeSpace();
				if(freeSpace > 0) {
					ioTask.setStatus(Status.FAIL_IO);
					LogUtil.exception(
						Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"",
						dstPath
					);
				} else {
					ioTask.setStatus(Status.RESP_FAIL_SPACE);
					LogUtil.exception(Level.DEBUG, e, "No free space for the path \"{}\"", dstPath);
				}
				return null;
			} catch(final IOException e) {
				ioTask.setStatus(Status.FAIL_IO);
				LogUtil.exception(
					Level.DEBUG, e, "Failed to open the output channel for the path \"{}\"", dstPath
				);
				return null;
			}
		};
		
		requestAuthTokenFunc = null; // do not use
	}
	
	@Override
	protected final String requestNewPath(final String path) {
		final File pathFile = FS.getPath(path).toFile();
		if(!pathFile.exists()) {
			if(!pathFile.mkdirs()) {
				return null;
			}
		}
		return path;
	}
	
	@Override
	protected final String requestNewAuthToken(final Credential credential) {
		throw new AssertionError("Should not be invoked");
	}
	
	@Override
	public final void adjustIoBuffers(final long avgTransferSize, final IoType ioType) {
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
						break;
					}
					if(srcChannel == null) {
						if(ioTask.getStatus().equals(Status.FAIL_IO)) {
							break;
						} else {
							invokeCreate(item, ioTask, dstChannel);
						}
					} else { // copy the data from the src channel to the dst channel
						invokeCopy(item, ioTask, srcChannel, dstChannel);
					}
					break;
				
				case READ:
					srcChannel = srcOpenFiles.computeIfAbsent(ioTask, openSrcFileFunc);
					if(srcChannel == null) {
						break;
					}
					final List<ByteRange> fixedByteRangesToRead = ioTask.getFixedRanges();
					if(verifyFlag) {
						try {
							if(fixedByteRangesToRead == null || fixedByteRangesToRead.isEmpty()) {
								if(ioTask.hasMarkedRanges()) {
									invokeReadAndVerifyRandomRanges(
										item, ioTask, srcChannel,
										ioTask.getMarkedRangesMaskPair()
									);
								} else {
									invokeReadAndVerify(item, ioTask, srcChannel);
								}
							} else {
								invokeReadAndVerifyFixedRanges(
									item, ioTask, srcChannel, fixedByteRangesToRead
								);
							}
						} catch(final DataSizeException e) {
							ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
							final long
								countBytesDone = ioTask.getCountBytesDone() + e.getOffset();
							ioTask.setCountBytesDone(countBytesDone);
							Loggers.MSG.debug(
								"{}: content size mismatch, expected: {}, actual: {}",
								item.getName(), item.size(), countBytesDone
							);
						} catch(final DataCorruptionException e) {
							ioTask.setStatus(Status.RESP_FAIL_CORRUPT);
							final long
								countBytesDone = ioTask.getCountBytesDone() + e.getOffset();
							ioTask.setCountBytesDone(countBytesDone);
							Loggers.MSG.debug(
								"{}: content mismatch @ offset {}, expected: {}, actual: {} ",
								item.getName(), countBytesDone,
								String.format("\"0x%X\"", (int) (e.expected & 0xFF)),
								String.format("\"0x%X\"", (int) (e.actual & 0xFF))
							);
						}
					} else {
						if(fixedByteRangesToRead == null || fixedByteRangesToRead.isEmpty()) {
							if(ioTask.hasMarkedRanges()) {
								invokeReadRandomRanges(
									item, ioTask, srcChannel, ioTask.getMarkedRangesMaskPair()
								);
							} else {
								invokeRead(item, ioTask, srcChannel);
							}
						} else {
							invokeReadFixedRanges(item, ioTask, srcChannel, fixedByteRangesToRead);
						}
					}
					break;
				
				case UPDATE:
					dstChannel = dstOpenFiles.computeIfAbsent(ioTask, openDstFileFunc);
					if(dstChannel == null) {
						break;
					}
					final List<ByteRange> fixedByteRangesToUpdate = ioTask.getFixedRanges();
					if(fixedByteRangesToUpdate == null || fixedByteRangesToUpdate.isEmpty()) {
						if(ioTask.hasMarkedRanges()) {
							invokeRandomRangesUpdate(item, ioTask, dstChannel);
						} else {
							throw new AssertionError("Not implemented");
						}
					} else {
						invokeFixedRangesUpdate(item, ioTask, dstChannel, fixedByteRangesToUpdate);
					}
					break;
				
				case DELETE:
					invokeDelete(ioTask);
					break;
				
				default:
					ioTask.setStatus(Status.FAIL_UNKNOWN);
					Loggers.ERR.fatal("Unknown load type \"{}\"", ioType);
					break;
			}
		} catch(final FileNotFoundException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
			ioTask.setStatus(Status.RESP_FAIL_NOT_FOUND);
		} catch(final AccessDeniedException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
			ioTask.setStatus(Status.RESP_FAIL_AUTH);
		} catch(final ClosedChannelException e) {
			ioTask.setStatus(Status.INTERRUPTED);
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, ioTask.toString());
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
	
	private void invokeCreate(
		final I fileItem, final O ioTask, final WritableByteChannel dstChannel
	) throws IOException {
		long countBytesDone = ioTask.getCountBytesDone();
		final long contentSize = fileItem.size();
		if(countBytesDone < contentSize && Status.ACTIVE.equals(ioTask.getStatus())) {
			countBytesDone += fileItem.write(dstChannel, contentSize - countBytesDone);
			ioTask.setCountBytesDone(countBytesDone);
		}
		if(countBytesDone == contentSize) {
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
		}
		if(countBytesDone == contentSize) {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadAndVerify(
		final I fileItem, final O ioTask, final ReadableByteChannel srcChannel
	) throws DataSizeException, IOException {
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
				Loggers.MSG.debug(
					"{}: content mismatch @ offset {}, expected: {}, actual: {} ",
					fileItem.getName(), countBytesDone,
					String.format("\"0x%X\"", (int) (e.expected & 0xFF)),
					String.format("\"0x%X\"", (int) (e.actual & 0xFF))
				);
			}
			ioTask.setCountBytesDone(countBytesDone);
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadAndVerifyRandomRanges(
		final I fileItem, final O ioTask, final SeekableByteChannel srcChannel,
		final BitSet maskRangesPair[]
	) throws DataSizeException, DataCorruptionException, IOException {

		long countBytesDone = ioTask.getCountBytesDone();
		final long rangesSizeSum = ioTask.getMarkedRangesSize();

		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {

			DataItem range2read;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.getCurrRangeIdx();
				if(currRangeIdx < getRangeCount(fileItem.size())) {
					if(maskRangesPair[0].get(currRangeIdx) || maskRangesPair[1].get(currRangeIdx)) {
						range2read = ioTask.getCurrRange();
						break;
					} else {
						ioTask.setCurrRangeIdx(++ currRangeIdx);
					}
				} else {
					ioTask.setCountBytesDone(rangesSizeSum);
					return;
				}
			}

			final long currRangeSize = range2read.size();
			srcChannel.position(getRangeOffset(currRangeIdx) + countBytesDone);
			countBytesDone += range2read.readAndVerify(
				srcChannel, ThreadLocalByteBuffer.get(currRangeSize - countBytesDone)
			);
			if(countBytesDone == currRangeSize) {
				ioTask.setCurrRangeIdx(currRangeIdx + 1);
				ioTask.setCountBytesDone(0);
			}
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadAndVerifyFixedRanges(
		final I fileItem, final O ioTask, final SeekableByteChannel srcChannel,
		final List<ByteRange> fixedRanges
	) throws DataSizeException, DataCorruptionException, IOException {
		
		final long baseItemSize = fileItem.size();
		final long fixedRangesSizeSum = ioTask.getMarkedRangesSize();
		
		long countBytesDone = ioTask.getCountBytesDone();
		// "countBytesDone" is the current range done bytes counter here
		long rangeBytesDone = countBytesDone;
		long currOffset;
		long cellOffset;
		long cellEnd;
		int n;
		
		if(fixedRangesSizeSum > 0 && fixedRangesSizeSum > countBytesDone) {

			ByteRange fixedRange;
			DataItem currRange;
			int currFixedRangeIdx = ioTask.getCurrRangeIdx();
			long fixedRangeEnd;
			long fixedRangeSize;

			if(currFixedRangeIdx < fixedRanges.size()) {
				fixedRange = fixedRanges.get(currFixedRangeIdx);
				currOffset = fixedRange.getBeg();
				fixedRangeEnd = fixedRange.getEnd();
				if(currOffset == -1) {
					// last "rangeEnd" bytes
					currOffset = baseItemSize - fixedRangeEnd;
					fixedRangeSize = fixedRangeEnd;
				} else if(fixedRangeEnd == -1) {
					// start @ offset equal to "rangeBeg"
					fixedRangeSize = baseItemSize - currOffset;
				} else {
					fixedRangeSize = fixedRangeEnd - currOffset + 1;
				}
				
				// let (current offset = rangeBeg + rangeBytesDone)
				currOffset += rangeBytesDone;
				// find the internal data item's cell index which has:
				// (cell's offset <= current offset) && (cell's end > current offset)
				n = getRangeCount(currOffset + 1) - 1;
				cellOffset = getRangeOffset(n);
				cellEnd = Math.min(baseItemSize, getRangeOffset(n + 1));
				// get the found cell data item (updated or not)
				currRange = fileItem.slice(cellOffset, cellEnd - cellOffset);
				if(fileItem.isRangeUpdated(n)) {
					currRange.layer(fileItem.layer() + 1);
				}
				// set the cell data item internal position to (current offset - cell's offset)
				currRange.position(currOffset - cellOffset);
				srcChannel.position(currOffset);
				rangeBytesDone += currRange.readAndVerify(
					srcChannel,
					ThreadLocalByteBuffer.get(
						Math.min(
							fixedRangeSize - countBytesDone, currRange.size() - currRange.position()
						)
					)
				);

				if(rangeBytesDone == fixedRangeSize) {
					// current byte range verification is finished
					if(currFixedRangeIdx == fixedRanges.size() - 1) {
						// current byte range was last in the list
						ioTask.setCountBytesDone(fixedRangesSizeSum);
						finishIoTask(ioTask);
						return;
					} else {
						ioTask.setCurrRangeIdx(currFixedRangeIdx + 1);
						rangeBytesDone = 0;
					}
				}
				ioTask.setCountBytesDone(rangeBytesDone);
			} else {
				ioTask.setCountBytesDone(fixedRangesSizeSum);
			}
		} else {
			finishIoTask(ioTask);
		}
	}
	
	private void invokeRead(final I fileItem, final O ioTask, final ReadableByteChannel srcChannel)
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
			} else {
				countBytesDone += n;
				ioTask.setCountBytesDone(countBytesDone);
			}
		}
		if(countBytesDone == contentSize) {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadRandomRanges(
		final I fileItem, final O ioTask, final FileChannel srcChannel,
		final BitSet maskRangesPair[]
	) throws IOException {
		
		int n;
		long countBytesDone = ioTask.getCountBytesDone();
		final long rangesSizeSum = ioTask.getMarkedRangesSize();
		
		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {
			
			DataItem range2read;
			int currRangeIdx;
			while(true) {
				currRangeIdx = ioTask.getCurrRangeIdx();
				if(currRangeIdx < getRangeCount(fileItem.size())) {
					if(maskRangesPair[0].get(currRangeIdx) || maskRangesPair[1].get(currRangeIdx)) {
						range2read = ioTask.getCurrRange();
						break;
					} else {
						ioTask.setCurrRangeIdx(++ currRangeIdx);
					}
				} else {
					ioTask.setCountBytesDone(rangesSizeSum);
					return;
				}
			}
			
			final long currRangeSize = range2read.size();
			n = srcChannel.read(
				ThreadLocalByteBuffer.get(currRangeSize - countBytesDone),
				getRangeOffset(currRangeIdx) + countBytesDone
			);
			if(n < 0) {
				finishIoTask(ioTask);
				ioTask.setCountBytesDone(countBytesDone);
				return;
			}
			countBytesDone += n;
			
			if(countBytesDone == currRangeSize) {
				ioTask.setCurrRangeIdx(currRangeIdx + 1);
				ioTask.setCountBytesDone(0);
			}
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeReadFixedRanges(
		final I fileItem, final O ioTask, final FileChannel srcChannel,
		final List<ByteRange> byteRanges
	) throws IOException {
		
		int n;
		long countBytesDone = ioTask.getCountBytesDone();
		final long baseItemSize = fileItem.size();
		final long rangesSizeSum = ioTask.getMarkedRangesSize();
		
		if(rangesSizeSum > 0 && rangesSizeSum > countBytesDone) {
			
			ByteRange byteRange;
			int currRangeIdx = ioTask.getCurrRangeIdx();
			long rangeBeg;
			long rangeEnd;
			long rangeSize;
			
			if(currRangeIdx < byteRanges.size()) {
				byteRange = byteRanges.get(currRangeIdx);
				rangeBeg = byteRange.getBeg();
				rangeEnd = byteRange.getEnd();
				if(rangeBeg == -1) {
					// last "rangeEnd" bytes
					rangeBeg = baseItemSize - rangeEnd;
					rangeSize = rangeEnd;
				} else if(rangeEnd == -1) {
					// start @ offset equal to "rangeBeg"
					rangeSize = baseItemSize - rangeBeg;
				} else {
					rangeSize = rangeEnd - rangeBeg + 1;
				}
				n = srcChannel.read(
					ThreadLocalByteBuffer.get(rangeSize - countBytesDone), rangeBeg + countBytesDone
				);
				if(n < 0) {
					finishIoTask(ioTask);
					ioTask.setCountBytesDone(countBytesDone);
					return;
				}
				countBytesDone += n;
				
				if(countBytesDone == rangeSize) {
					ioTask.setCurrRangeIdx(currRangeIdx + 1);
					ioTask.setCountBytesDone(0);
				} else {
					ioTask.setCountBytesDone(countBytesDone);
				}
			} else {
				ioTask.setCountBytesDone(rangesSizeSum);
			}
		} else {
			finishIoTask(ioTask);
		}
	}

	private void invokeRandomRangesUpdate(
		final I fileItem, final O ioTask, final SeekableByteChannel dstChannel
	) throws IOException {
		
		long countBytesDone = ioTask.getCountBytesDone();
		final long updatingRangesSize = ioTask.getMarkedRangesSize();
		
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
			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
					"{}: set the file position = {} + {}", fileItem.getName(),
					getRangeOffset(currRangeIdx), countBytesDone
				);
			}
			dstChannel.position(getRangeOffset(currRangeIdx) + countBytesDone);
			countBytesDone += updatingRange.write(dstChannel, updatingRangeSize - countBytesDone);
			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace(
					"{}: {} bytes written totally", fileItem.getName(), countBytesDone
				);
			}
			if(countBytesDone == updatingRangeSize) {
				ioTask.setCurrRangeIdx(currRangeIdx + 1);
				ioTask.setCountBytesDone(0);
			} else {
				ioTask.setCountBytesDone(countBytesDone);
			}
		} else {
			if(Loggers.MSG.isTraceEnabled()) {
				Loggers.MSG.trace("{}: {} bytes updated", fileItem.getName(), updatingRangesSize);
			}
			finishIoTask(ioTask);
			fileItem.commitUpdatedRanges(ioTask.getMarkedRangesMaskPair());
		}
	}

	private void invokeFixedRangesUpdate(
		final I fileItem, final O ioTask, final SeekableByteChannel dstChannel,
		final List<ByteRange> byteRanges
	) throws IOException {

		long countBytesDone = ioTask.getCountBytesDone();
		final long baseItemSize = fileItem.size();
		final long updatingRangesSize = ioTask.getMarkedRangesSize();

		if(updatingRangesSize > 0 && updatingRangesSize > countBytesDone) {

			ByteRange byteRange;
			DataItem updatingRange;
			int currRangeIdx = ioTask.getCurrRangeIdx();
			long rangeBeg;
			long rangeEnd;
			long rangeSize;

			if(currRangeIdx < byteRanges.size()) {
				byteRange = byteRanges.get(currRangeIdx);
				rangeBeg = byteRange.getBeg();
				rangeEnd = byteRange.getEnd();
				rangeSize = byteRange.getSize();
				if(rangeSize == -1) {
					if(rangeBeg == -1) {
						// last "rangeEnd" bytes
						rangeBeg = baseItemSize - rangeEnd;
						rangeSize = rangeEnd;
					} else if(rangeEnd == -1) {
						// start @ offset equal to "rangeBeg"
						rangeSize = baseItemSize - rangeBeg;
					} else {
						rangeSize = rangeEnd - rangeBeg + 1;
					}
				} else {
					// append
					rangeBeg = baseItemSize;
					// note down the new size
					fileItem.size(baseItemSize + updatingRangesSize);
				}
				updatingRange = fileItem.slice(rangeBeg, rangeSize);
				updatingRange.position(countBytesDone);
				dstChannel.position(rangeBeg + countBytesDone);
				countBytesDone += updatingRange.write(dstChannel, rangeSize - countBytesDone);

				if(countBytesDone == rangeSize) {
					ioTask.setCurrRangeIdx(currRangeIdx + 1);
					ioTask.setCountBytesDone(0);
				} else {
					ioTask.setCountBytesDone(countBytesDone);
				}

			} else {
				ioTask.setCountBytesDone(updatingRangesSize);
			}
		} else {
			finishIoTask(ioTask);
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
