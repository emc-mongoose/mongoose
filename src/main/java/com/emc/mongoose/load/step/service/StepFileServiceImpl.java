package com.emc.mongoose.load.step.service;

import com.emc.mongoose.load.step.StepFile;
import com.emc.mongoose.load.step.StepFileImpl;
import com.emc.mongoose.svc.ServiceBase;
import com.emc.mongoose.load.step.StepFileService;

import java.io.IOException;
import java.nio.file.OpenOption;

public final class StepFileServiceImpl
extends ServiceBase
implements StepFileService {

	private final StepFile file;

	public StepFileServiceImpl(final String filePath, final int port) {
		super(port);
		file = new StepFileImpl(filePath);
	}

	@Override
	public final void open(final OpenOption[] openOptions)
	throws IOException {
		file.open(openOptions);
	}

	@Override
	public final byte[] read()
	throws IOException {
		return file.read();
	}

	@Override
	public final void write(final byte[] buff)
	throws IOException {
		file.write(buff);
	}

	@Override
	public final long position()
	throws IOException {
		return file.position();
	}

	@Override
	public final void position(final long newPosition)
	throws IOException {
		file.position(newPosition);
	}

	@Override
	public final long size()
	throws IOException {
		return file.size();
	}

	@Override
	public final void truncate(final long size)
	throws IOException {
		file.truncate(size);
	}

	@Override
	public final void closeFile()
	throws IOException {
		file.closeFile();
	}

	@Override
	public final String filePath()
	throws IOException {
		return file.filePath();
	}

	@Override
	public final String name() {
		try {
			final String filePath = file.filePath();
			return SVC_NAME_PREFIX + (filePath.startsWith("/") ? filePath : ("/" + filePath));
		} catch(final IOException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	protected final void doClose()
	throws IOException {
		file.delete();
	}
}
