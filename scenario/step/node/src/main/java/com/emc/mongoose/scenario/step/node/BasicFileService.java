package com.emc.mongoose.scenario.step.node;

import static com.github.akurilov.commons.system.DirectMemUtil.REUSABLE_BUFF_SIZE_MAX;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.step.FileService;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.Level;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Paths;

public final class BasicFileService
extends ServiceBase
implements FileService {

	private volatile SeekableByteChannel fileChannel = null;
	private final String filePath;

	public BasicFileService(final String filePath, final int port) {
		super(port);
		if(filePath == null) {
			if(!Files.exists(TMP_DIR)) {
				try {
					Files.createDirectories(TMP_DIR);
				} catch(final IOException e) {
					LogUtil.exception(Level.DEBUG, e, "Failed to create tmp directory {}", TMP_DIR);
				}
			}
			this.filePath = Paths
				.get(TMP_DIR.toString(), Long.toString(System.nanoTime()))
				.toString();
		} else {
			this.filePath = filePath;
		}
		new File(this.filePath).deleteOnExit();
	}

	@Override
	public final void open(final OpenOption[] openOptions)
	throws IOException {
		if(fileChannel != null) {
			throw new IllegalStateException("File \"" + filePath + "\" is already open");
		}
		this.fileChannel = Files.newByteChannel(Paths.get(filePath), openOptions);
	}

	@Override
	public final byte[] read()
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		final long remainingSize = fileChannel.size() - fileChannel.position();
		if(remainingSize <= 0) {
			throw new EOFException();
		}
		final int buffSize = (int) Math.min(REUSABLE_BUFF_SIZE_MAX, remainingSize);
		if(buffSize > 0) {
			final ByteBuffer bb = ByteBuffer.allocate(buffSize);
			int doneSize = 0;
			int n;
			while(doneSize < buffSize) {
				n = fileChannel.read(bb);
				if(n < 0) {
					// unexpected but possible: the file is shorter than was estimated before
					final byte[] buff = new byte[bb.position()];
					bb.rewind();
					bb.get(buff);
					return buff;
				} else {
					doneSize += n;
				}
			}
			return bb.array();
		} else {
			return EMPTY;
		}
	}

	@Override
	public final void write(final byte[] buff)
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		final ByteBuffer bb = ByteBuffer.wrap(buff);
		int n = 0;
		while(n < buff.length) {
			n = fileChannel.write(bb);
		}
	}

	@Override
	public final long position()
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		return fileChannel.position();
	}

	@Override
	public final void position(final long newPosition)
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		fileChannel.position(newPosition);
	}

	@Override
	public final long size()
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		return fileChannel.size();
	}

	@Override
	public final void truncate(final long size)
	throws IOException {
		if(fileChannel == null) {
			throw new IllegalStateException();
		}
		fileChannel.truncate(size);
	}

	@Override
	public final void closeFile()
	throws IOException {
		if(fileChannel != null) {
			fileChannel.close();
			fileChannel = null;
		}
	}

	@Override
	public final String filePath() {
		return filePath;
	}

	@Override
	public final String name() {
		return SVC_NAME_PREFIX + (filePath.startsWith("/") ? filePath : ("/" + filePath));
	}

	@Override
	protected final void doClose()
	throws IOException {
		closeFile();
		Files.delete(Paths.get(filePath));
	}
}
