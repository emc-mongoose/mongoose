package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.DataLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.DataLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.DataItemFileSrc;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
import com.emc.mongoose.core.api.io.task.IOTask;
//
import com.emc.mongoose.server.api.load.builder.DataLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.DataLoadSvc;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.rmi.RemoteException;
/**
 Created by kurila on 20.10.15.
 */
public abstract class DataLoadBuilderClientBase<
	T extends DataItem,
	W extends DataLoadSvc<T>,
	U extends DataLoadClient<T, W>,
	V extends DataLoadBuilderSvc<T, W>
>
extends LoadBuilderClientBase<T, W, U, V>
implements DataLoadBuilderClient<T, W, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected long minObjSize, maxObjSize;
	protected float objSizeBias;
	protected boolean flagUseContainerItemSrc;
	//
	protected DataLoadBuilderClientBase()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	protected DataLoadBuilderClientBase(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		super.setAppConfig(appConfig);
		final AppConfig.DataSizeScheme dataSizeScheme = appConfig.getItemDataSizeClass();
		if(AppConfig.DataSizeScheme.FIXED.equals(dataSizeScheme)) {
			setMinObjSize(appConfig.getItemDataSizeFixed());
			setMaxObjSize(appConfig.getItemDataSizeFixed());
			setObjSizeBias(0);
		} else {
			setMinObjSize(appConfig.getItemDataSizeRandomMin());
			setMaxObjSize(appConfig.getItemDataSizeRandomMax());
			setObjSizeBias((float) appConfig.getItemDataSizeRandomBias());
		}
		return this;
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setMinObjSize(final long minObjSize)
	throws IllegalArgumentException, RemoteException {
		this.minObjSize = minObjSize;
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setMinObjSize(minObjSize);
			}
		}
		return this;
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setObjSizeBias(final float objSizeBias)
	throws IllegalArgumentException, RemoteException {
		this.objSizeBias = objSizeBias;
		V nextBuilder;
		if(loadSvcMap != null) {
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setObjSizeBias(objSizeBias);
			}
		}
		return this;
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setMaxObjSize(final long maxObjSize)
	throws IllegalArgumentException, RemoteException {
		this.maxObjSize = maxObjSize;
		if(loadSvcMap != null) {
			V nextBuilder;
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setMaxObjSize(maxObjSize);
			}
		}
		return this;
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setRandomRangesCount(int count)
	throws RemoteException {
		if(loadSvcMap != null) {
			V nextBuilder;
			for(final String addr : loadSvcMap.keySet()) {
				nextBuilder = loadSvcMap.get(addr);
				nextBuilder.setRandomRangesCount(count);
			}
		}
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	public DataLoadBuilderClient<T, W, U> setItemSrc(final ItemSrc<T> itemSrc)
	throws RemoteException {
		super.setItemSrc(itemSrc);
		//
		if(itemSrc instanceof DataItemFileSrc) {
			// calculate approx average data item size
			final DataItemFileSrc<T> fileInput = (DataItemFileSrc<T>) itemSrc;
			final long approxDataItemsSize = fileInput.getApproxDataItemsSize(
				appConfig.getItemInputBatchSize()
			);
			ioConfig.setBuffSize(
				approxDataItemsSize < Constants.BUFF_SIZE_LO ?
					Constants.BUFF_SIZE_LO :
					approxDataItemsSize > Constants.BUFF_SIZE_HI ?
						Constants.BUFF_SIZE_HI : (int) approxDataItemsSize
			);
		}
		return this;
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected ItemSrc<T> getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				// disable any item source usage on the load servers side
				V nextBuilder;
				for(final String addr : loadSvcMap.keySet()) {
					nextBuilder = loadSvcMap.get(addr);
					nextBuilder.useNoneItemSrc();
				}
				//
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.WRITE.equals(ioConfig.getLoadType())) {
					// enable new data item generation on the load servers side
					V nextBuilder;
					for(final String addr : loadSvcMap.keySet()) {
						nextBuilder = loadSvcMap.get(addr);
						nextBuilder.useNewItemSrc();
					}
					//
					return null;
				} else {
					// disable any item source usage on the load servers side
					V nextBuilder;
					for(final String addr : loadSvcMap.keySet()) {
						nextBuilder = loadSvcMap.get(addr);
						nextBuilder.useNoneItemSrc();
					}
					//
					return (ItemSrc<T>) ((IOConfig) ioConfig.clone()).getContainerListInput(
						maxCount, storageNodeAddrs == null ? null : storageNodeAddrs[0]
					);
				}
			} else if(flagUseNewItemSrc) {
				// enable new data item generation on the load servers side
				V nextBuilder;
				for(final String addr : loadSvcMap.keySet()) {
					nextBuilder = loadSvcMap.get(addr);
					nextBuilder.useNewItemSrc();
				}
				//
				return null;
			} else if(flagUseContainerItemSrc) {
				// disable any item source usage on the load servers side
				V nextBuilder;
				for(final String addr : loadSvcMap.keySet()) {
					nextBuilder = loadSvcMap.get(addr);
					nextBuilder.useNoneItemSrc();
				}
				//
				return (ItemSrc<T>) ((IOConfig) ioConfig.clone()).getContainerListInput(
					maxCount, storageNodeAddrs == null ? null : storageNodeAddrs[0]
				);
			}
		} catch(final RemoteException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to change the remote data items source");
		} catch(final CloneNotSupportedException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to clone the I/O config instance");
		}
		return null;
	}
	//
	@Override
	protected final void resetItemSrc() {
		super.resetItemSrc();
		flagUseContainerItemSrc = true;
	}
}
