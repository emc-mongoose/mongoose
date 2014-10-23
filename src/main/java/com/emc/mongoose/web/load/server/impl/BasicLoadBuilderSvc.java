package com.emc.mongoose.web.load.server.impl;
//
import com.emc.mongoose.object.load.server.ObjectLoadSvc;
import com.emc.mongoose.web.data.WSObject;
import com.emc.mongoose.web.load.impl.BasicLoadBuilder;
import com.emc.mongoose.web.load.server.WSLoadBuilderSvc;
import com.emc.mongoose.web.load.server.impl.type.AppendSvc;
import com.emc.mongoose.web.load.server.impl.type.CreateSvc;
import com.emc.mongoose.web.load.server.impl.type.DeleteSvc;
import com.emc.mongoose.web.load.server.impl.type.ReadSvc;
import com.emc.mongoose.web.load.server.impl.type.UpdateSvc;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.Locale;
/**
 Created by kurila on 30.05.14.
 */
public class BasicLoadBuilderSvc<T extends WSObject, U extends WSLoadExecutor<T>>
extends BasicLoadBuilder<T, U>
implements WSLoadBuilderSvc<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private RunTimeConfig clientConfig = null;
	//
	@Override
	public final com.emc.mongoose.web.load.server.WSLoadBuilderSvc<T, U> setProperties(final RunTimeConfig clientConfig) {
		super.setProperties(clientConfig);
		this.clientConfig = clientConfig;
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
	@Override @SuppressWarnings("unchecked")
	public final U build()
	throws IllegalStateException {
		if(clientConfig== null) {
			throw new IllegalStateException("Should upload properties to the server before instancing");
		}
		if(reqConf == null) {
			throw new IllegalStateException("Should specify request builder instance before instancing");
		}
		//
		ObjectLoadSvc<T> loadSvc = null;
		final WSRequestConfig wsReqConf = WSRequestConfig.class.cast(reqConf);
		if(minObjSize <= maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						LOG.debug("New create load");
						if(minObjSize > maxObjSize) {
							throw new IllegalStateException(
								"Min object size should be not more than max object size"
							);
						}
						loadSvc = new CreateSvc<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						loadSvc = new ReadSvc<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						loadSvc = new UpdateSvc<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						loadSvc = new DeleteSvc<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						loadSvc = new AppendSvc<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							minObjSize, maxObjSize
						);
				}
			} catch(final CloneNotSupportedException | IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			throw new IllegalStateException(
				String.format(
					Locale.ROOT, "Min object size %s should be less than upper bound %s",
					RunTimeConfig.formatSize(minObjSize), RunTimeConfig.formatSize(maxObjSize)
				)
			);
		}
		//
		return (U) loadSvc;
	}
	//
	public void start() {
		LOG.info(Markers.MSG, "Load builder service instance created");
		/*final RemoteStub stub = */ServiceUtils.create(this);
		/*LOG.debug(Markers.MSG, stub.toString());*/
		LOG.info(Markers.MSG, "Server started and waiting for the requests");
	}
	//
	@Override
	public final void close()
	throws IOException {
	}
}
