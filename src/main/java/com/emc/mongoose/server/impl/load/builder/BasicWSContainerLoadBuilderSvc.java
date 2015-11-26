package com.emc.mongoose.server.impl.load.builder;

import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicWSContainerLoadBuilder;
import com.emc.mongoose.server.api.load.builder.WSContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.WSContainerLoadSvc;
import com.emc.mongoose.server.impl.load.executor.BasicWSContainerLoadSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;

/**
 * Created by gusakk on 22.10.15.
 */
public class BasicWSContainerLoadBuilderSvc<
	T extends WSObject,
	C extends Container<T>,
	U extends WSContainerLoadSvc<T, C>
>
extends BasicWSContainerLoadBuilder<T, C, U>
implements WSContainerLoadBuilderSvc<T, C, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private String configTable = null;
	//
	public BasicWSContainerLoadBuilderSvc(final RunTimeConfig runTimeConfig) {
		super(runTimeConfig);
	}
	//
	@Override
	public final BasicWSContainerLoadBuilderSvc<T, C, U>
	setProperties(final RunTimeConfig clientConfig) {
		super.setProperties(clientConfig);
		final String runMode = clientConfig.getRunMode();
		if (!runMode.equals(Constants.RUN_MODE_SERVER)
				&& !runMode.equals(Constants.RUN_MODE_COMPAT_SERVER)) {
			configTable = clientConfig.toString();
		}
		RunTimeConfig.getContext();
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final String buildRemotely()
	throws RemoteException {
		final WSContainerLoadSvc<T, C> loadSvc = build();
		ServiceUtil.create(loadSvc);
		if(configTable != null) {
			LOG.info(Markers.MSG, configTable);
			configTable = null;
		}
		return loadSvc.getName();
	}
	//
	@Override
	public final String getName() {
		return getClass().getName();
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
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(ioConfig);
		final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
		// the statement below fixes hi-level API distributed mode usage and tests
		localRunTimeConfig.setProperty(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_SERVER);
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int
			connPerNode = loadTypeConnPerNode.get(loadType),
			minThreadCount = getMinIOThreadCount(
				loadTypeWorkerCount.get(loadType), storageNodeAddrs.length, connPerNode
			);
		//
		return (U) new BasicWSContainerLoadSvc<>(
			localRunTimeConfig, wsReqConf, storageNodeAddrs, connPerNode, minThreadCount,
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
		ServiceUtil.close(this);
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
