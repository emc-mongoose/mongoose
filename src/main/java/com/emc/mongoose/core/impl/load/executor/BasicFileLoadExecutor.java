package com.emc.mongoose.core.impl.load.executor;
//
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.load.IoTask;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.concurrent.Future;
/**
 Created by andrey on 11.04.16.
 */
public class BasicFileLoadExecutor<F extends FileItem, A extends IoTask<F>>
implements FileLoadExecutor<F, A> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicFileLoadExecutor() {

	}
	//
	@Override
	public Future<A> submit(final A ioTask)
	throws RemoteException {
		return null;
	}
	//
	@Override
	public int submit(final List<A> requests, final int from, final int to)
	throws RemoteException {
		return 0;
	}
	//
	@Override
	public void close()
	throws IOException {
	}
}
