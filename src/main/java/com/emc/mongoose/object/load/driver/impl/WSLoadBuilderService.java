package com.emc.mongoose.object.load.driver.impl;
//
import com.emc.mongoose.base.api.Request;
import com.emc.mongoose.base.api.RequestConfig;
import com.emc.mongoose.base.load.LoadBuilder;
import com.emc.mongoose.base.load.LoadExecutor;
import com.emc.mongoose.object.load.driver.ObjectLoadBuilderService;
import com.emc.mongoose.object.load.driver.ObjectLoadService;
import com.emc.mongoose.object.load.driver.impl.type.WSAppendService;
import com.emc.mongoose.object.load.driver.impl.type.WSCreateService;
import com.emc.mongoose.object.load.driver.impl.type.WSDeleteService;
import com.emc.mongoose.object.load.driver.impl.type.WSReadService;
import com.emc.mongoose.object.load.driver.impl.type.WSUpdateService;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.object.load.impl.WSLoadBuilder;
import com.emc.mongoose.object.data.WSDataObject;
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
public class WSLoadBuilderService<T extends WSDataObject, U extends LoadExecutor<T>>
extends WSLoadBuilder<T, U>
implements ObjectLoadBuilderService<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private RunTimeConfig clientProps = null;
	//
	@Override @SuppressWarnings("unchecked")
	public final String buildRemotely()
	throws RemoteException {
		final ObjectLoadService<T> loadSvc = (ObjectLoadService<T>) build();
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
			throw new IllegalStateException("Should upload properties to the driver before");
		}
		if(reqConf==null) {
			throw new IllegalStateException("Should specify request builder instance");
		}
		//
		ObjectLoadService<T> loadSvc = null;
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
						loadSvc = new WSCreateService(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							minObjSize, maxObjSize
						);
						break;
					case READ:
						LOG.debug("New read load");
						loadSvc = new WSReadService<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case UPDATE:
						LOG.debug("New update load");
						loadSvc = new WSUpdateService<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType),
							updatesPerItem
						);
						break;
					case DELETE:
						LOG.debug("New delete load");
						loadSvc = new WSDeleteService<>(
							dataNodeAddrs, reqConf, maxCount, threadsPerNodeMap.get(loadType)
						);
						break;
					case APPEND:
						LOG.debug("New append load");
						loadSvc = new WSAppendService<>(
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
	public static void run() {
		final WSLoadBuilderService loadBuilderSvc = new WSLoadBuilderService<>();
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
