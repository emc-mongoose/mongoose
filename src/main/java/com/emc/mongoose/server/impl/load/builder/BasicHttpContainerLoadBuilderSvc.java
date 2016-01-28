package com.emc.mongoose.server.impl.load.builder;

import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicHttpContainerLoadBuilder;
import com.emc.mongoose.server.api.load.builder.HttpContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.HttpContainerLoadSvc;
import com.emc.mongoose.server.impl.load.executor.BasicHttpContainerLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Created by gusakk on 22.10.15.
 */
public class BasicHttpContainerLoadBuilderSvc<
	T extends HttpDataItem,
	C extends Container<T>,
	U extends HttpContainerLoadSvc<T, C>
>
extends BasicHttpContainerLoadBuilder<T, C, U>
implements HttpContainerLoadBuilderSvc<T, C, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static AtomicInteger FORK_COUNTER = new AtomicInteger(0);
	//
	private String name = getClass().getName();
	//
	public BasicHttpContainerLoadBuilderSvc(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	public final int fork()
	throws RemoteException {
		try {
			final BasicHttpContainerLoadBuilderSvc<T, C, U>
				forkedSvc = (BasicHttpContainerLoadBuilderSvc<T, C, U>) clone();
			final int forkNum = FORK_COUNTER.getAndIncrement();
			forkedSvc.name = name + forkNum;
			forkedSvc.start();
			LOG.info(Markers.MSG, "Service \"" + name + "\" started");
			return forkNum;
		} catch(final CloneNotSupportedException e) {
			throw new RemoteException(e.toString());
		}
	}
	//
	@Override
	public String buildRemotely()
	throws RemoteException {
		U loadSvc = build();
		LOG.info(Markers.MSG, appConfig.toString());
		ServiceUtil.create(loadSvc);
		return loadSvc.getName();
	}
	//
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final int getNextInstanceNum(final String runId) {
		return LoadExecutor.NEXT_INSTANCE_NUM.get();
	}
	//
	@Override
	public final void setNextInstanceNum(final String runId, final int instanceN) {
		LoadExecutor.NEXT_INSTANCE_NUM.set(instanceN);
	}
	//
	@Override
	public final void invokePreConditions() {} // discard any precondition invocations in load server mode
	//
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws IllegalStateException {
		if(ioConfig == null) {
			throw new IllegalStateException("Should specify request builder instance before instancing");
		}
		//
		final HttpRequestConfig wsReqConf = HttpRequestConfig.class.cast(ioConfig);
		final AppConfig localAppConfig = BasicConfig.THREAD_CONTEXT.get();
		// the statement below fixes hi-level API distributed mode usage and tests
		localAppConfig.setProperty(AppConfig.KEY_RUN_MODE, Constants.RUN_MODE_SERVER);
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int
			connPerNode = loadTypeConnPerNode.get(loadType),
			minThreadCount = getMinIOThreadCount(
				loadTypeWorkerCount.get(loadType), storageNodeAddrs.length, connPerNode
			);
		//
		return (U) new BasicHttpContainerLoadSvc<>(
			localAppConfig, wsReqConf, storageNodeAddrs, connPerNode, minThreadCount,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, manualTaskSleepMicroSecs, rateLimit
		);
	}
	//
	public final void start()
	throws RemoteException {
		LOG.debug(Markers.MSG, "Load builder service instance created");
		try {
			/*final RemoteStub stub = */
			ServiceUtil.create(this);
			/*LOG.debug(Markers.MSG, stub.toString());*/
		} catch (final DuplicateSvcNameException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Possible load service usage collision");
		}
		LOG.info(Markers.MSG, "Server started and waiting for the requests");
	}
	//
	@Override
	public void shutdown()
	throws RemoteException, IllegalStateException {
	}
	//
	@Override
	public void await()
	throws RemoteException, InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public void await(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException, InterruptedException {
		timeUnit.sleep(timeOut);
	}
	//
	@Override
	public void interrupt()
	throws RemoteException {
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
		ServiceUtil.close(this);
		LOG.info(Markers.MSG, "Service \"{}\" closed", name);
	}
	//
	@Override
	protected boolean itemsFileExists(final String filePathStr) {
		if(filePathStr != null && !filePathStr.isEmpty()) {
			final Path listFilePath = Paths.get(filePathStr);
			if(!Files.exists(listFilePath)) {
				LOG.debug(Markers.MSG, "Specified input file \"{}\" doesn't exists", listFilePath);
			} else if(!Files.isReadable(listFilePath)) {
				LOG.debug(Markers.MSG, "Specified input file \"{}\" isn't readable", listFilePath);
			} else if(Files.isDirectory(listFilePath)) {
				LOG.debug(Markers.MSG, "Specified input file \"{}\" is a directory", listFilePath);
			} else {
				return true;
			}
		}
		return false;
	}
}
