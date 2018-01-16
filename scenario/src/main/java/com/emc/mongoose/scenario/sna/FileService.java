package com.emc.mongoose.scenario.sna;

import com.emc.mongoose.api.model.svc.Service;

import java.io.IOException;

public interface FileService
extends Service {

	String SVC_NAME_PREFIX = "file/";
	int BUFF_SIZE = 0x2000;

	byte[] read()
	throws IOException;

	int write(final byte[] buff)
	throws IOException;
}
