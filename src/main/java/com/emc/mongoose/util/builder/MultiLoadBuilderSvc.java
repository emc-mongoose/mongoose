package com.emc.mongoose.util.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.builder.BasicDirectoryLoadBuilder;
import com.emc.mongoose.core.impl.load.builder.BasicWSContainerLoadBuilder;
import com.emc.mongoose.server.api.load.builder.LoadBuilderSvc;
//
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
import com.emc.mongoose.server.impl.load.builder.BasicDirectoryLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicFileLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSContainerLoadBuilderSvc;
import com.emc.mongoose.server.impl.load.builder.BasicWSDataLoadBuilderSvc;
//
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
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
	public MultiLoadBuilderSvc(final RunTimeConfig rtConfig) {
		loadBuilderSvcs.add(new BasicWSContainerLoadBuilderSvc(rtConfig));
		loadBuilderSvcs.add(new BasicWSDataLoadBuilderSvc(rtConfig));
		loadBuilderSvcs.add(
			new BasicDirectoryLoadBuilderSvc<
				FileItem, Directory<FileItem>,
				DirectoryLoadSvc<FileItem, Directory<FileItem>>
			>(rtConfig)
		);
		loadBuilderSvcs.add(new BasicFileLoadBuilderSvc<>(rtConfig));
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
	public final void await()
	throws RemoteException, InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		final ExecutorService awaitExecutor = Executors.newFixedThreadPool(loadBuilderSvcs.size());
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			awaitExecutor.submit(
				new Runnable() {
					@Override
					public final void run() {
						try {
							loadBuilderSvc.await(timeOut, timeUnit);
						} catch(final InterruptedException | RemoteException ignored) {
						}
					}
				}
			);
		}
		awaitExecutor.shutdown();
		try {
			awaitExecutor.awaitTermination(timeOut, timeUnit);
		} finally {
			awaitExecutor.shutdownNow();
		}
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
	public final LoadBuilderSvc setProperties(final RunTimeConfig props)
	throws IllegalStateException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setProperties(props);
		}
		return this;
	}
	//
	@Override
	public final IOConfig getIOConfig()
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	@Override
	public LoadBuilder setIOConfig(final IOConfig ioConfig)
	throws RemoteException {
		throw new RemoteException("Method shouldn't be invoked");
	}
	//
	@Override
	public final LoadBuilderSvc setLoadType(final IOTask.Type loadType)
	throws IllegalStateException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setLoadType(loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setMaxCount(final long maxCount)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setMaxCount(maxCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setManualTaskSleepMicroSecs(final int manualTaskSleepMicroSec)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setManualTaskSleepMicroSecs(manualTaskSleepMicroSec);
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
	public final LoadBuilderSvc setWorkerCountDefault(final int threadCount)
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setWorkerCountDefault(threadCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setWorkerCountFor(final int threadCount, final IOTask.Type loadType)
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setWorkerCountFor(threadCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setConnPerNodeDefault(final int connCount)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setConnPerNodeDefault(connCount);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setConnPerNodeFor(final int connCount, final IOTask.Type loadType)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setConnPerNodeFor(connCount, loadType);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setDataNodeAddrs(final String[] dataNodeAddrs)
	throws IllegalArgumentException, RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setDataNodeAddrs(dataNodeAddrs);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc setItemSrc(final ItemSrc itemSrc)
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.setItemSrc(itemSrc);
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc useNewItemSrc()
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.useNewItemSrc();
		}
		return this;
	}
	//
	@Override
	public final LoadBuilderSvc useNoneItemSrc()
	throws RemoteException {
		for(final LoadBuilderSvc loadBuilderSvc : loadBuilderSvcs) {
			loadBuilderSvc.useNoneItemSrc();
		}
		return null;
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


