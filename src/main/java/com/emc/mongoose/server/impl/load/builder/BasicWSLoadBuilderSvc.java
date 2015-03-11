package com.emc.mongoose.server.impl.load.builder;
//
import com.emc.mongoose.core.impl.load.builder.BasicWSLoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.load.executor.LoadExecutorBase;
import com.emc.mongoose.server.api.persist.DataItemBufferSvc;
import com.emc.mongoose.server.api.load.executor.ObjectLoadSvc;
import com.emc.mongoose.server.impl.load.executor.BasicWSLoadSvc;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.server.api.load.builder.WSLoadBuilderSvc;
import com.emc.mongoose.core.api.io.req.conf.WSRequestConfig;
import com.emc.mongoose.core.api.load.executor.WSLoadExecutor;
import com.emc.mongoose.core.impl.util.RunTimeConfig;
import com.emc.mongoose.core.api.persist.Markers;
import com.emc.mongoose.server.impl.ServiceUtils;
//
import com.emc.mongoose.server.impl.persist.TmpFileItemBufferSvc;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 */
public class BasicWSLoadBuilderSvc<T extends WSObject, U extends WSLoadExecutor<T>>
extends BasicWSLoadBuilder<T, U>
implements WSLoadBuilderSvc<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSLoadBuilderSvc(final RunTimeConfig runTimeConfig) {
		super(runTimeConfig);
	}
	//
	@Override
	public final WSLoadBuilderSvc<T, U> setProperties(final RunTimeConfig clientConfig) {
		super.setProperties(clientConfig);
		RunTimeConfig.getContext();
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final String buildRemotely()
	throws RemoteException {
		final ObjectLoadSvc<T> loadSvc = (ObjectLoadSvc<T>) build();
		ServiceUtils.create(loadSvc);
		return loadSvc.getName();
	}
	//
	@Override
	public final String getName() {
		return "//"+ServiceUtils.getHostAddr()+'/'+getClass().getPackage().getName();
	}
	//
	@Override
	public final int getLastInstanceNum() {
		return LoadExecutor.LAST_INSTANCE_NUM.get();
	}
	//
	@Override
	public final void setLastInstanceNum(final int instanceN) {
		LoadExecutorBase.LAST_INSTANCE_NUM.set(instanceN);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final U build()
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
					Main.LOCALE_DEFAULT, "Min object size %s should be less than upper bound %s",
					RunTimeConfig.formatSize(minObjSize), RunTimeConfig.formatSize(maxObjSize)
				)
			);
		}
		return (U) new BasicWSLoadSvc<>(
			localRunTimeConfig, wsReqConf, dataNodeAddrs, threadsPerNodeMap.get(loadType),
			listFile, maxCount, minObjSize, maxObjSize, objSizeBias, updatesPerItem
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public DataItemBufferSvc<T> newDataItemBuffer()
	throws IOException {
		return (DataItemBufferSvc<T>) ServiceUtils.create(
			new TmpFileItemBufferSvc<>(getMaxCount(), 1)
		);
	}
	//
	public final void start() {
		LOG.debug(Markers.MSG, "Load builder service instance created");
		/*final RemoteStub stub = */ServiceUtils.create(this);
		/*LOG.debug(Markers.MSG, stub.toString());*/
		LOG.info(Markers.MSG, "Server started and waiting for the requests");
	}
	//
	@Override
	public final void join()
	throws InterruptedException {
		join(Long.MAX_VALUE);
	}
	//
	@Override
	public final void join(final long ms)
	throws InterruptedException {
		Thread.sleep(ms);
	}
	//
	@Override
	public final void close()
	throws IOException {
	}
}
