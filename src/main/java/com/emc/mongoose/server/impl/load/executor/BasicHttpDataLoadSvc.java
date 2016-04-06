package com.emc.mongoose.server.impl.load.executor;
// mongoose-common.jar
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
import com.emc.mongoose.common.net.http.conn.pool.HttpConnPool;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.HttpDataItem;
import com.emc.mongoose.core.api.item.base.Output;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.HttpRequestConfig;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.load.executor.BasicHttpDataLoadExecutor;
// mongoose-server-api.jar
import com.emc.mongoose.server.api.load.executor.HttpDataLoadSvc;
//
import org.apache.http.HttpHost;
import org.apache.http.impl.nio.pool.BasicNIOPoolEntry;
import org.apache.http.nio.protocol.HttpAsyncRequester;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.protocol.HttpProcessor;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.LockSupport;

/**
 Created by kurila on 16.12.14.
 */
public class BasicHttpDataLoadSvc<T extends HttpDataItem>
extends BasicHttpDataLoadExecutor<T>
implements HttpDataLoadSvc<T> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicHttpDataLoadSvc(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadsPerNode, final ItemSrc<T> itemSrc,
		final long maxCount, final float rateLimit,
		final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) {
		super(
			appConfig, reqConfig, addrs, threadsPerNode, itemSrc, maxCount, rateLimit,
			sizeConfig, rangesConfig
		);
	}
	//
	public BasicHttpDataLoadSvc(
		final AppConfig appConfig, final HttpRequestConfig<T, ? extends Container<T>> reqConfig,
		final String[] addrs, final int threadCount, final ItemSrc<T> itemSrc, final long maxCount,
		final float rateLimit, final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig,
		final HttpProcessor httpProcessor, final HttpAsyncRequester client,
		final ConnectingIOReactor ioReactor,
		final Map<HttpHost, HttpConnPool<HttpHost, BasicNIOPoolEntry>> connPoolMap
	) {
		super(
			appConfig, reqConfig, addrs, threadCount, itemSrc, maxCount, rateLimit,
			sizeConfig, rangesConfig, httpProcessor, client, ioReactor, connPoolMap
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
	public final void setOutput(final Output<T> itemOutput) {
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
					super.setOutput((Output<T>) localSvc);
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
	protected final void postProcessUniqueItemsFinally(final List<T> items) {
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
	public final int getInstanceNum() {
		return instanceNum;
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
	public int getProcessedItemsCount()
	throws RemoteException {
		return itemOutBuff.size();
	}
}
