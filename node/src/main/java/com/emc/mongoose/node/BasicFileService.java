package com.emc.mongoose.node;

import static com.github.akurilov.commons.system.DirectMemUtil.REUSABLE_BUFF_SIZE_MAX;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.FileService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class BasicFileService
extends ServiceBase
implements FileService {

	private final FileChannel fileChannel;
	private final String filePath;

	public BasicFileService(final String filePath, final int port)
	throws IOException {
		super(port);
		if(filePath == null) {
			final File tmpFile = File.createTempFile("mongoose.node.", ".tmp");
			this.filePath = SVC_NAME_PREFIX + tmpFile.getAbsolutePath();
			this.fileChannel = FileChannel.open(tmpFile.toPath(), WRITE, TRUNCATE_EXISTING);
		} else {
			this.filePath = SVC_NAME_PREFIX + filePath;
			this.fileChannel = FileChannel.open(Paths.get(filePath), READ, NOFOLLOW_LINKS);
		}
	}

	@Override
	public final byte[] read()
	throws IOException {
		final long remainingSize = fileChannel.size() - fileChannel.position();
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
		final ByteBuffer bb = ByteBuffer.wrap(buff);
		int n = 0;
		while(n < buff.length) {
			n = fileChannel.write(bb);
		}
	}

	@Override
	public final String getFilePath() {
		return filePath;
	}

	@Override
	public final String getName() {
		return SVC_NAME_PREFIX + filePath;
	}

	@Override
	protected final void doClose()
	throws IOException {
		fileChannel.close();
		Files.delete(Paths.get(filePath));
	}
}
