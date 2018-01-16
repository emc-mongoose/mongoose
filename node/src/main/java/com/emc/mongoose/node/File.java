package com.emc.mongoose.node;

import com.emc.mongoose.api.model.svc.ServiceBase;
import com.emc.mongoose.scenario.sna.RemoteFile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.rmi.RemoteException;

public final class File
extends ServiceBase
implements RemoteFile {

	private final FileChannel fileChannel;

	public File(final int port) {
		super(port);
	}

	@Override
	public String getName()
	throws RemoteException {
		return null;
	}

	@Override
	public boolean isOpen() {
		return false;
	}

	@Override
	public final void close()
	throws IOException {
		super.close();
		fileChannel.close();
	}

	@Override
	public int read(final ByteBuffer dst)
	throws IOException {
		return ;
	}

	@Override
	public int write(final ByteBuffer src)
	throws IOException {
		return 0;
	}
}
