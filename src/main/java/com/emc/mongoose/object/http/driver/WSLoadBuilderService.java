package com.emc.mongoose.object.http.driver;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.object.http.WSLoadBuilder;
import com.emc.mongoose.object.http.data.WSObject;
import com.emc.mongoose.object.http.driver.impl.CreateService;
import com.emc.mongoose.object.http.driver.impl.DeleteService;
import com.emc.mongoose.object.http.driver.impl.ReadService;
import com.emc.mongoose.object.http.driver.impl.UpdateService;
import com.emc.mongoose.remote.LoadBuilderService;
import com.emc.mongoose.remote.LoadService;
import com.emc.mongoose.remote.Service;
import com.emc.mongoose.remote.ServiceUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 30.05.14.
 */
public class WSLoadBuilderService<T extends LoadService<WSObject>>
extends WSLoadBuilder<T>
implements LoadBuilderService<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private RunTimeConfig clientProps = null;
	//
	@Override
	public final LoadBuilderService<T> setProperties(final RunTimeConfig clientProps) {
		super.setProperties(clientProps);
		this.clientProps = clientProps;
		return this;
	}
	//
	@Override
	public final String buildRemotely()
	throws RemoteException {
		final T loadSvc = build();
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
	public final T build()
	throws IllegalStateException {
		if(clientProps==null) {
			throw new IllegalStateException("Should upload properties to the driver before");
		}
		if(reqConf==null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		LoadService<WSObject> loadSvc = null;
		if(minObjSize<=maxObjSize) {
			try {
				switch(loadType) {
					case CREATE:
						LOG.debug("New create load");
						if(minObjSize>maxObjSize) {
							throw new IllegalStateException(
								"Min object size should be not more than max object size"
							);
						}
						loadSvc = new CreateService(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						loadSvc = new ReadService(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						loadSvc = new UpdateService(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile, updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						loadSvc = new DeleteService(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							listFile
						);
						break;
				}
			} catch(CloneNotSupportedException|IOException e) {
				throw new IllegalStateException(e);
			}
		} else {
			throw new IllegalStateException(
				"Min object size ("+Long.toString(minObjSize)+
					") should be less than upper bound "+Long.toString(maxObjSize)
			);
		}
		//
		return (T) loadSvc;
	}
	//
	public static void run() {
		final Service loadBuilderSvc = new WSLoadBuilderService();
		LOG.info(Markers.MSG, "Load builder service instance created");
		/*final RemoteStub stub = */ServiceUtils.create(loadBuilderSvc);
		/*LOG.debug(Markers.MSG, stub.toString());*/
		LOG.info(Markers.MSG, "Driver started and waiting for the requests");
	}
	//
	@Override
	public final void close()
	throws IOException {
	}
}
