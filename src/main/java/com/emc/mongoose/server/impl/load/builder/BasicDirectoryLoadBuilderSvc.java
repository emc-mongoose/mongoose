package com.emc.mongoose.server.impl.load.builder;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.container.Directory;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.load.builder.BasicDirectoryLoadBuilder;
//
import com.emc.mongoose.server.api.load.builder.DirectoryLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.DirectoryLoadSvc;
//
import com.emc.mongoose.server.impl.load.executor.BasicDirectoryLoadSvc;
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
/**
 Created by kurila on 26.11.15.
 */
public class BasicDirectoryLoadBuilderSvc<
	T extends FileItem,
	C extends Directory<T>,
	U extends DirectoryLoadSvc<T, C>
>
extends BasicDirectoryLoadBuilder<T, C, U>
implements DirectoryLoadBuilderSvc<T, C, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private String configTable = null;
	//
	public BasicDirectoryLoadBuilderSvc(final RunTimeConfig rtConfig)
	throws RemoteException {
		super(rtConfig);
	}
	//
	@Override
	public final BasicDirectoryLoadBuilderSvc<T, C, U> setProperties(
		final RunTimeConfig clientConfig
	) throws RemoteException {
		super.setProperties(clientConfig);
		final String runMode = clientConfig.getRunMode();
		if(
			!runMode.equals(Constants.RUN_MODE_SERVER) &&
			!runMode.equals(Constants.RUN_MODE_COMPAT_SERVER)
		) {
			configTable = clientConfig.toString();
		}
		RunTimeConfig.getContext();
		return this;
	}
	//
	@Override
	public String buildRemotely()
	throws RemoteException {
		U loadSvc = build();
		ServiceUtil.create(loadSvc);
		if(configTable != null) {
			LOG.info(Markers.MSG, configTable);
			configTable = null;
		}
		return loadSvc.getName();
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws IllegalStateException {
		if(ioConfig == null) {
			throw new IllegalStateException("Should specify request builder instance before instancing");
		}
		//
		final RunTimeConfig rtConfig = RunTimeConfig.getContext();
		// the statement below fixes hi-level API distributed mode usage and tests
		rtConfig.setProperty(RunTimeConfig.KEY_RUN_MODE, Constants.RUN_MODE_SERVER);
		//
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int connPerNode = loadTypeConnPerNode.get(loadType);
		//
		return (U) new BasicDirectoryLoadSvc<>(
			rtConfig, (FileIOConfig<T, C>) ioConfig, storageNodeAddrs, connPerNode, connPerNode,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, manualTaskSleepMicroSecs, rateLimit
		);
	}
	//
	@Override
	public int getNextInstanceNum(final String runId)
	throws RemoteException {
		return LoadExecutor.NEXT_INSTANCE_NUM.get();
	}
	//
	@Override
	public void setNextInstanceNum(final String runId, final int instanceN)
	throws RemoteException {
		LoadExecutor.NEXT_INSTANCE_NUM.set(instanceN);
	}
	//
	@Override
	public String getName()
	throws RemoteException {
		return getClass().getName();
	}
	//
	@Override
	public void start()
	throws RemoteException, IllegalThreadStateException {
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
	public void interrupt() throws RemoteException {
	}
	//
	@Override
	public final void close()
	throws IOException {
		super.close();
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
