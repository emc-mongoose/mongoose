package com.emc.mongoose.server.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.WSObject;
import com.emc.mongoose.core.api.data.model.ItemDst;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.req.WSRequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.executor.BasicWSDataLoadExecutor;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.WSDataLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
/**
 Created by kurila on 16.12.14.
 */
public final class BasicWSDataLoadSvc<T extends WSObject>
extends BasicWSDataLoadExecutor<T>
implements WSDataLoadSvc<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicWSDataLoadSvc(
		final RunTimeConfig runTimeConfig, final WSRequestConfig<T> reqConfig, final String[] addrs,
		final int connPerNode, final int threadsPerNode,
		final ItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final int manualTaskSleepMicroSecs, final float rateLimit, final int countUpdPerReq
	) {
		super(
			runTimeConfig, reqConfig, addrs, connPerNode, threadsPerNode, itemSrc, maxCount,
			sizeMin, sizeMax, sizeBias, manualTaskSleepMicroSecs, rateLimit, countUpdPerReq
		);
	}
	//
	@Override
	protected void closeActually()
	throws IOException {
		try {
			super.closeActually();
		} finally {
			// close the exposed network service, if any
			final Service svc = ServiceUtil.getLocalSvc(ServiceUtil.getLocalSvcName(getName()));
			if(svc == null) {
				LOG.debug(Markers.MSG, "The load was not exposed remotely");
			} else {
				LOG.debug(Markers.MSG, "The load was exposed remotely, removing the service");
				ServiceUtil.close(svc);
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void setItemDst(final ItemDst<T> itemDst) {
		LOG.debug(
			Markers.MSG, "Set consumer {} for {}, trying to resolve local service from the name",
			itemDst, getName()
		);
		try {
			if(itemDst instanceof Service) {
				final String remoteSvcName = ((Service) itemDst).getName();
				LOG.debug(Markers.MSG, "Name is {}", remoteSvcName);
				final Service localSvc = ServiceUtil.getLocalSvc(
					ServiceUtil.getLocalSvcName(remoteSvcName)
				);
				if(localSvc == null) {
					LOG.error(
						Markers.ERR, "Failed to get local service for name \"{}\"",
						remoteSvcName
					);
				} else {
					super.setItemDst((ItemDst<T>) localSvc);
					LOG.debug(
						Markers.MSG,
						"Successfully resolved local service and appended it as consumer"
					);
				}
			} else {
				super.setItemDst(itemDst);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "{}: looks like network failure", getName());
		}
	}
	// prevent output buffer consuming by the logger at the end of a chain
	@Override
	protected final void passItems()
	throws InterruptedException {
		if(consumer != null) {
			super.passItems();
		}
	}
	//
	@Override
	public final List<T> getProcessedItems()
	throws RemoteException {
		List<T> itemsBuff = null;
		try {
			itemsBuff = new ArrayList<>(DEFAULT_RESULTS_QUEUE_SIZE);
			itemOutBuff.get(itemsBuff, DEFAULT_RESULTS_QUEUE_SIZE);
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failed to get the buffered items");
		}
		return itemsBuff;
	}
	//
	@Override
	public final int getInstanceNum() {
		return instanceNum;
	}
	//
	@Override
	public int getProcessedItemsCount()
	throws RemoteException {
		return itemOutBuff.size();
	}
}
