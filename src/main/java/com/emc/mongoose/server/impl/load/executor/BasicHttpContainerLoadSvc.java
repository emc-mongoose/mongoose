package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
//
import com.emc.mongoose.core.impl.load.executor.BasicHttpContainerLoadExecutor;
//
import com.emc.mongoose.server.api.load.executor.HttpContainerLoadSvc;
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
 Created by kurila on 21.10.15.
 */
public class BasicHttpContainerLoadSvc<T extends HttpDataItem, C extends Container<T>>
extends BasicHttpContainerLoadExecutor<T, C>
implements HttpContainerLoadSvc<T, C> {
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpContainerLoadSvc(
		final AppConfig appConfig, final HttpRequestConfig reqConfig, final String[] addrs,
		final int threadsPerNode, final Input<C> itemInput,
		final long countCount, final long sizeLimit, final float rateLimit
	) {
		super(
			appConfig, reqConfig, addrs, threadsPerNode, itemInput, countCount, sizeLimit, rateLimit
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
			LOG.debug(Markers.MSG, "The load was exposed remotely, removing the service");
			ServiceUtil.close(this);
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	public final void setOutput(final Output<C> itemOutput) {
		LOG.debug(
			Markers.MSG, "Set consumer {} for {}, trying to resolve local service from the name",
			itemOutput, getName()
		);
		try {
			if(itemOutput instanceof Service) {
				final String remoteSvcUrl = ((Service)itemOutput).getName();
				LOG.debug(Markers.MSG, "Name is {}", remoteSvcUrl);
				final Service localSvc = ServiceUtil.getLocalSvc(
					ServiceUtil.getSvcUrl(remoteSvcUrl)
				);
				if(localSvc == null) {
					LOG.error(
						Markers.ERR, "Failed to get local service for name \"{}\"",
						remoteSvcUrl
					);
				} else {
					super.setOutput((Output<C>) localSvc);
					LOG.debug(
						Markers.MSG,
						"Successfully resolved local service and appended it as consumer"
					);
				}
			} else {
				super.setOutput(itemOutput);
			}
		} catch(final IOException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "{}: looks like network failure", getName());
		}
	}
	// prevent output buffer consuming by the logger at the end of a chain
	@Override
	protected final void postProcessItems()
	throws InterruptedException {
		if(consumer != null) {
			super.postProcessItems();
		}
	}
	//
	@Override
	protected void postProcessUniqueItemsFinally(final List<C> items) {
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
					for(int m = 0; m < n; m += consumer.put(items, m, n)) {
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
	public final List<C> getProcessedItems()
	throws RemoteException {
		List<C> itemsBuff = null;
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
