package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

import java.io.IOException;

public interface FileService
extends Service {

	String SVC_NAME_PREFIX = "file/";
	byte[] EMPTY = new byte[0];

	byte[] read()
	throws IOException;

	void write(final byte[] buff)
	throws IOException;
}
