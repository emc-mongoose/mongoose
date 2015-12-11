package com.emc.mongoose.server.impl.load.builder;
//
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.load.builder.BasicFileLoadBuilder;
import com.emc.mongoose.server.api.load.builder.FileLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
import com.emc.mongoose.server.impl.load.executor.BasicFileLoadSvc;
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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadBuilderSvc<T extends FileItem, U extends FileLoadSvc<T>>
extends BasicFileLoadBuilder<T, U>
implements FileLoadBuilderSvc<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final Lock remoteLock = new ReentrantLock();
	//
	public BasicFileLoadBuilderSvc(final RunTimeConfig rtConfig)
	throws RemoteException {
		super(rtConfig);
	}
	//
	@Override
	public final boolean lockUntilSvcBuilt(final long timeOut, final TimeUnit timeUnit)
	throws RemoteException {
		try {
			return remoteLock.tryLock(timeOut, timeUnit);
		} catch(final InterruptedException e) {
			throw new RemoteException(e.toString());
		}
	}
	//
	@Override
	public String buildRemotely()
	throws RemoteException {
		U loadSvc;
		try {
			LOG.info(Markers.MSG, rtConfig.toString());
			loadSvc = build();
		} finally {
			remoteLock.unlock();
		}
		ServiceUtil.create(loadSvc);
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
		if(minObjSize > maxObjSize) {
			throw new IllegalStateException(
				String.format(
					LogUtil.LOCALE_DEFAULT, "Min object size %s should be less than upper bound %s",
					SizeUtil.formatSize(minObjSize), SizeUtil.formatSize(maxObjSize)
				)
			);
		}
		//
		final IOTask.Type loadType = ioConfig.getLoadType();
		final int connPerNode = loadTypeConnPerNode.get(loadType);
		//
		return (U) new BasicFileLoadSvc<>(
			rtConfig, (FileIOConfig) ioConfig, storageNodeAddrs, connPerNode, connPerNode,
			itemSrc == null ? getDefaultItemSource() : itemSrc,
			maxCount, minObjSize, maxObjSize, objSizeBias,
			manualTaskSleepMicroSecs, rateLimit, updatesPerItem
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
	public void interrupt()
	throws RemoteException {
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
