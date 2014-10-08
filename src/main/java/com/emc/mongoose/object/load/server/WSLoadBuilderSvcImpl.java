package com.emc.mongoose.object.load.server;
//
import com.emc.mongoose.object.data.WSObjectImpl;
import com.emc.mongoose.object.load.WSLoadExecutor;
import com.emc.mongoose.object.load.server.type.ws.AppendSvc;
import com.emc.mongoose.object.load.server.type.ws.CreateSvc;
import com.emc.mongoose.object.load.server.type.ws.DeleteSvc;
import com.emc.mongoose.object.load.server.type.ws.ReadSvc;
import com.emc.mongoose.object.load.server.type.ws.UpdateSvc;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.WSLoadBuilderImpl;
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
	private RunTimeConfig clientProps = null;
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
		return "//"+ServiceUtils.getHostAddr()+'/'+getClass().getSimpleName();
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final U build()
	throws IllegalStateException {
		if(clientProps==null) {
			throw new IllegalStateException("Should upload properties to the server before");
		}
		if(reqConf==null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		ObjectLoadSvc<T> loadSvc = null;
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
						loadSvc = new CreateSvc<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						loadSvc = new ReadSvc<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						loadSvc = new UpdateSvc<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						loadSvc = new DeleteSvc<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						loadSvc = new AppendSvc<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
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
		LOG.info(Markers.MSG, "Driver started and waiting for the requests");
	}
	//
	@Override
	public final void close()
	throws IOException {
	}
}
