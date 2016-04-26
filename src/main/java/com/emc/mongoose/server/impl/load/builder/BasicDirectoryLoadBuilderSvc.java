package com.emc.mongoose.server.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
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
import java.util.concurrent.atomic.AtomicInteger;
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
	private final static AtomicInteger FORK_COUNTER = new AtomicInteger(0);
	//
	private String name = getClass().getName();
	//
	public BasicDirectoryLoadBuilderSvc(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	public final int fork()
	throws RemoteException {
		try {
			final BasicDirectoryLoadBuilderSvc<T, C, U>
				forkedSvc = (BasicDirectoryLoadBuilderSvc<T, C, U>) clone();
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
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws IllegalStateException {
		if(ioConfig == null) {
			throw new IllegalStateException("Should specify request builder instance before instancing");
		}
		// the statement below fixes hi-level API distributed mode usage and tests
		appConfig.setProperty(AppConfig.KEY_RUN_MODE, Constants.RUN_MODE_SERVER);
		//
		return (U) new BasicDirectoryLoadSvc<>(
			appConfig, (FileIoConfig<T, C>) ioConfig, threadCount,
			itemInput == null ? getDefaultItemInput() : itemInput, countLimit, sizeLimit, rateLimit
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
	//
	@Override
	public final String getName() {
		return name;
	}
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
