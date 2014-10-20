package com.emc.mongoose.web.load.server;
//
import com.emc.mongoose.object.load.server.ObjectLoadSvc;
import com.emc.mongoose.web.load.server.type.AppendSvcImpl;
import com.emc.mongoose.web.load.server.type.CreateSvcImpl;
import com.emc.mongoose.web.load.server.type.DeleteSvcImpl;
import com.emc.mongoose.web.load.server.type.ReadSvcImpl;
import com.emc.mongoose.web.load.server.type.UpdateSvcImpl;
import com.emc.mongoose.web.api.WSRequestConfig;
import com.emc.mongoose.web.data.WSObjectImpl;
import com.emc.mongoose.web.load.WSLoadExecutor;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.web.load.WSLoadBuilderImpl;
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
public class WSLoadBuilderSvcImpl<T extends WSObjectImpl, U extends WSLoadExecutor<T>>
extends WSLoadBuilderImpl<T, U>
implements WSLoadBuilderSvc<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private RunTimeConfig clientConfig = null;
	//
	@Override
	public final WSLoadBuilderSvc<T, U> setProperties(final RunTimeConfig clientConfig) {
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
						loadSvc = new CreateSvcImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						loadSvc = new ReadSvcImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						loadSvc = new UpdateSvcImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType),
							updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						loadSvc = new DeleteSvcImpl<T>(
							runTimeConfig,
							dataNodeAddrs, wsReqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						loadSvc = new AppendSvcImpl<T>(
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
