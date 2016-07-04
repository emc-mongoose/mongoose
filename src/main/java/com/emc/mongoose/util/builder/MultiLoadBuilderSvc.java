package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.concurrent.NamingThreadFactory;
import com.emc.mongoose.common.conf.AppConfig;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicDirectoryLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicFileLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicHttpContainerLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicHttpDataLoadBuilderSvc;
//
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
/**
 * Created by gusakk on 23.10.15.
 */
public class MultiLoadBuilderSvc
implements LoadBuilderSvc {
	//
	private static final Logger LOG = org.apache.logging.log4j.LogManager.getLogger();
	//
	private final List<LoadBuilderSvc> loadBuilderSvcs = new ArrayList<>();
	//
	public MultiLoadBuilderSvc(final AppConfig appConfig)
	throws RemoteException {
		loadBuilderSvcs.add(new BasicHttpContainerLoadBuilderSvc(appConfig));
		loadBuilderSvcs.add(new BasicHttpDataLoadBuilderSvc(appConfig));
		loadBuilderSvcs.add(
			new BasicDirectoryLoadBuilderSvc<
				FileItem, Directory<FileItem>,
				DirectoryLoadSvc<FileItem, Directory<FileItem>>
			>(appConfig)
		);
		loadBuilderSvcs.add(new BasicFileLoadBuilderSvc<>(appConfig));
	}
	//
	@Override
	public int fork()
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final String buildRemotely()
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final int getNextInstanceNum(final String runId)
	throws RemoteException {
		return LoadExecutor.NEXT_INSTANCE_NUM.get();
	}
	//
	@Override
	public final void setNextInstanceNum(final String runId, final int instanceN)
	throws RemoteException {
		LoadExecutor.NEXT_INSTANCE_NUM.set(instanceN);
	}
	//
	@Override
	public final void start()
	throws RemoteException, IllegalThreadStateException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.start();
		}
	}
	//
	@Override
	public final void shutdown()
	throws RemoteException, IllegalStateException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.shutdown();
		}
	}
	//
	@Override
	public final boolean await()
	throws RemoteException, InterruptedException {
		return await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public final boolean await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {

		long ts = System.currentTimeMillis();
		long timeOutMilliSec = timeUnit.toSeconds(timeOut);
		timeOutMilliSec = timeOutMilliSec > 0 ? timeOutMilliSec : Long.MAX_VALUE;

		LoadBuilderSvc loadBuilderSvc;

		while(!loadBuilderSvcs.isEmpty() && System.currentTimeMillis() - ts < timeOutMilliSec) {
			for(final Iterator<LoadBuilderSvc> it = loadBuilderSvcs.listIterator(); it.hasNext();) {
				loadBuilderSvc = it.next();
				if(loadBuilderSvc.await(1, TimeUnit.SECONDS)) {
					it.remove();
				}
			}
		}

		return loadBuilderSvcs.isEmpty();
	}
	//
	@Override
	public final void interrupt()
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.interrupt();
		}
	}
	//
	@Override
	public final LoadBuilderSvc setAppConfig(final AppConfig props)
	throws IllegalStateException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setAppConfig(props);
		}
		return this;
	}
	//
	@Override
	public final IoConfig getIoConfig()
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	@Override
	public LoadBuilder setIoConfig(final IoConfig ioConfig)
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final LoadBuilderSvc setLoadType(final LoadType loadType)
	throws IllegalStateException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setCountLimit(final long countLimit)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setCountLimit(countLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setSizeLimit(final long sizeLimit)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setSizeLimit(sizeLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setRateLimit(final float rateLimit)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setRateLimit(rateLimit);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setThreadCount(final int threadCount)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setThreadCount(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setNodeAddrs(final String[] nodeAddrs)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setNodeAddrs(nodeAddrs);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setInput(final Input itemInput)
	throws RemoteException {
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setOutput(final Output itemOutput)
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setOutput(itemOutput);
		}
		return this;
	}
	//
	@Override
	public void invokePreConditions()
	throws RemoteException, IllegalStateException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final LoadExecutor build()
	throws IOException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final String getName() throws RemoteException {
		return getClass().getName();
	}
	//
	@Override
	public final void close()
	throws IOException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.close();
		}
		loadBuilderSvcs.clear();
	}
	//
}


