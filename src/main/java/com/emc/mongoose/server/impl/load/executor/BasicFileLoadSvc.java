package com.emc.mongoose.server.impl.load.executor;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.Service;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.common.io.Output;
import com.emc.mongoose.core.api.io.conf.FileIoConfig;
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
		final AppConfig appConfig, final FileIoConfig<T, ? extends Directory<T>> ioConfig,
		final int threadCount, final Input<T> itemInput, final long maxCount,
		final float rateLimit, final SizeInBytes sizeConfig, final DataRangesConfig rangesConfig
	) throws ClassCastException {
		super(
			appConfig, ioConfig, threadCount, itemInput, maxCount, rateLimit, sizeConfig, rangesConfig
		);
	}
	//
	@Override
	protected void closeActually()
	throws IOException {
		try {
			super.closeActually();
		} finally {
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
