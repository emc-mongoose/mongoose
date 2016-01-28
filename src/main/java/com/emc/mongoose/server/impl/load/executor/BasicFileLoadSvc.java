package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
//
import com.emc.mongoose.core.impl.load.executor.BasicFileLoadExecutor;
//
import com.emc.mongoose.server.api.load.executor.FileLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadSvc<T extends FileItem>
extends BasicFileLoadExecutor<T>
implements FileLoadSvc<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicFileLoadSvc(
		final AppConfig appConfig, final FileIOConfig<T, ? extends Directory<T>> ioConfig,
		final String[] addrs, final int connCountPerNode, final int threadCount,
		final ItemSrc<T> itemSrc, final long maxCount,
		final long sizeMin, final long sizeMax, final float sizeBias,
		final int manualTaskSleepMicroSecs, final float rateLimit, final int countUpdPerReq
	) throws ClassCastException {
		super(appConfig, ioConfig, addrs, connCountPerNode, threadCount, itemSrc, maxCount, sizeMin, sizeMax, sizeBias, manualTaskSleepMicroSecs, rateLimit, countUpdPerReq);
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
	protected final void passUniqueItemsFinally(final List<T> items) {
		if(consumer != null) {
			int n = items.size();
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG, "Going to put {} items to the consumer {}",
					n, consumer
				);
			}
			try {
				if(!items.isEmpty()) {
					int m = 0, k;
					while(m < n) {
						k = consumer.put(items, m, n);
						if(k > 0) {
							m += k;
						}
						Thread.yield();
						LockSupport.parkNanos(1);
					}
				}
				items.clear();
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.DEBUG, e, "Failed to feed the items to \"{}\"", consumer
				);
			}
			if(LOG.isTraceEnabled(Markers.MSG)) {
				LOG.trace(
					Markers.MSG,
					"{} items were passed to the consumer {} successfully",
					n, consumer
				);
			}
		}
	}
	//
	@Override
	public int getInstanceNum()
	throws RemoteException {
		return instanceNum;
	}
	//
	@Override
	public List<T> getProcessedItems()
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
	public int getProcessedItemsCount()
	throws RemoteException {
		return itemOutBuff.size();
	}
}
