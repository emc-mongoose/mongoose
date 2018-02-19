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
	Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "mongoose");

	void open(final OpenOption[] openOptions)
	throws IOException;

	byte[] read()
	throws IOException;

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
