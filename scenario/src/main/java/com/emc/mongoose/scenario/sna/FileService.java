package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

import java.io.IOException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;

public interface FileService
extends Service {

	String SVC_NAME_PREFIX = "file";
	byte[] EMPTY = new byte[0];
	OpenOption[] READ_OPTIONS = new OpenOption[] {
		StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS
	};
	OpenOption[] WRITE_OPEN_OPTIONS = new OpenOption[] {
		StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING
	};
	OpenOption[] APPEND_OPEN_OPTIONS = new OpenOption[] {
		StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND
	};
	Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "mongoose");

	/**
	 Open the remote file for subsequent read either write operations
	 @param openOptions
	 @throws RemoteException the cause may be IllegalStateException if the file is open already
	 either IOException if failed to open file
	 */
	void open(final OpenOption[] openOptions)
	throws IOException;

	/**
	 Read the next batch of bytes from the remote file
	 @return the bytes been read, may return empty buffer in case of EOF
	 @throws RemoteException the cause may IllegalStateException if the file isn't open yet either
	 EOFException if end of file is reached
	 */
	byte[] read()
	throws IOException;

	/**
	 Write some bytes to the remote file. Blocks until all the bytes are written.
	 @param buff the bytes to write to the remote file
	 @throws RemoteException  the cause may IllegalStateException if the file isn't open yet either
	 another IOException
	 */
	void write(final byte[] buff)
	throws IOException;

	long position()
	throws IOException;

	void position(final long newPosition)
	throws IOException;

	long size()
	throws IOException;

	void truncate(final long size)
	throws IOException;

	void closeFile()
	throws IOException;

	String filePath()
	throws RemoteException;
}
