package com.emc.mongoose.server.impl.load.builder;
//mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.conf.SizeUtil;
import com.emc.mongoose.common.exceptions.DuplicateSvcNameException;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//mongoose-core-api.jar
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
//mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.WSLoadSvc;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.builder.BasicWSLoadBuilder;
// mongoose-server-impl.jar
import com.emc.mongoose.server.impl.load.executor.BasicWSLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.rmi.RemoteException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 Created by kurila on 30.05.14.
 */
public class BasicWSLoadBuilderSvc<T extends WSObject, U extends WSLoadExecutor<T>>
extends BasicWSLoadBuilder<T, U>
implements WSLoadBuilderSvc<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private String configTable = null;
	//
	public BasicWSLoadBuilderSvc(final RunTimeConfig runTimeConfig) {
		super(runTimeConfig);
	}
	//
	@Override
	public final WSLoadBuilderSvc<T, U> setProperties(final RunTimeConfig clientConfig) {
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
		final WSLoadSvc<T> loadSvc = (WSLoadSvc<T>) build();
		try {
			ServiceUtils.create(loadSvc);
			if (configTable != null) {
				LOG.info(Markers.MSG, configTable);
				configTable = null;
			}
		} catch (final DuplicateSvcNameException e) {
			throw new DuplicateSvcNameException();
		}
		return loadSvc.getName();
	}
	//
	@Override
	public final String getName() {
		return getClass().getPackage().getName();
	}
	//
	@Override
	public final int getNextInstanceNum(final String runId) {
		if (!LoadExecutor.INSTANCE_NUMBERS.containsKey(runId)) {
			LoadExecutor.INSTANCE_NUMBERS.put(runId, new AtomicInteger(0));
		}
		return LoadExecutor.INSTANCE_NUMBERS.get(runId).get();
	}
	//
	@Override
	public final void setNextInstanceNum(final String runId, final int instanceN) {
		LoadExecutor.INSTANCE_NUMBERS.get(runId).set(instanceN);
	}
	//
	@Override
	protected final void invokePreConditions() {} // discard any precondition invocations in load server mode
	//
	@Override @SuppressWarnings("unchecked")
	protected final U buildActually()
	throws IllegalStateException {
		if(reqConf == null) {
			throw new IllegalStateException("Should specify request builder instance before instancing");
		}
		//
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
		final RunTimeConfig localRunTimeConfig = RunTimeConfig.getContext();
		if(minObjSize > maxObjSize) {
			throw new IllegalStateException(
				String.format(
					LogUtil.LOCALE_DEFAULT, "Min object size %s should be less than upper bound %s",
					SizeUtil.formatSize(minObjSize), SizeUtil.formatSize(maxObjSize)
				)
			);
		}
		//
		return (U) new BasicWSLoadSvc<>(
			localRunTimeConfig, wsReqConf, dataNodeAddrs,
			threadsPerNodeMap.get(reqConf.getLoadType()), listFile,
			maxCount, minObjSize, maxObjSize, objSizeBias, rateLimit, updatesPerItem
		);
	}
	//
	public final void start()
	throws RemoteException {
		LOG.debug(Markers.MSG, "Load builder service instance created");
		try {
		/*final RemoteStub stub = */
			ServiceUtils.create(this);
		/*LOG.debug(Markers.MSG, stub.toString());*/
		} catch (final DuplicateSvcNameException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Service is busy by another client");
		}
		LOG.info(Markers.MSG, "Server started and waiting for the requests");
	}
	//
	@Override
	public final void await()
	throws InterruptedException {
		await(Long.MAX_VALUE, TimeUnit.DAYS);
	}
	//
	@Override
	public final void await(final long timeOut, final TimeUnit timeUnit)
	throws InterruptedException {
		timeUnit.sleep(timeOut);
	}
}
