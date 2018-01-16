package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.FileService;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;

public final class BasicFileService
extends ServiceBase
implements FileService {

	private final FileChannel fileChannel;
	private final String filePath;

	public BasicFileService(final int port, final String filePath)
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
	public final String getName()
	throws RemoteException {
		return SVC_NAME_PREFIX + filePath;
	}

	@Override
	public final byte[] read()
	throws IOException {
		final ByteBuffer bb = ByteBuffer.allocate(BUFF_SIZE);

		return bb.array();
	}

	@Override
	public final int write(final byte[] buff)
	throws IOException {
		return fileChannel.write(ByteBuffer.wrap(buff));
	}

	@Override
	public final void close()
	throws IOException {
		super.close();
		fileChannel.close();
		Files.delete(Paths.get(filePath));
	}
}
