package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.DataLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.DataLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.DataItemFileSrc;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.conf.IOConfig;
//
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.impl.item.base.BasicItemNameGenerator;
import com.emc.mongoose.core.impl.item.data.NewDataItemSrc;
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
	protected SizeInBytes sizeConfig;
	protected DataRangesConfig rangesConfig = null;
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
	public DataLoadBuilderClientBase<T, W, U, V> clone()
	throws CloneNotSupportedException {
		final DataLoadBuilderClientBase<T, W, U, V>
			lb = (DataLoadBuilderClientBase<T, W, U, V>) super.clone();
		lb.sizeConfig = sizeConfig;
		lb.rangesConfig = rangesConfig;
		return lb;
	}
	//
	@Override
	public final DataLoadBuilderClient<T, W, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		super.setAppConfig(appConfig);
		setDataSize(appConfig.getItemDataSize());
		setDataRanges(appConfig.getItemDataRanges());
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
			final long approxDataItemsSize = fileInput.getAvgDataSize(
				appConfig.getItemSrcBatchSize()
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
	protected ItemSrc<T> getNewItemSrc()
	throws NoSuchMethodException {
		final ItemNamingType namingType = appConfig.getItemNamingType();
		final BasicItemNameGenerator bing = new BasicItemNameGenerator(
			namingType,
			appConfig.getItemNamingPrefix(), appConfig.getItemNamingLength(),
			appConfig.getItemNamingRadix(), appConfig.getItemNamingOffset()
		);
		return new NewDataItemSrc<>(
			(Class<T>) ioConfig.getItemClass(), bing, ioConfig.getContentSource(), sizeConfig
		);
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected ItemSrc<T> getDefaultItemSrc() {
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
				if(LoadType.WRITE.equals(ioConfig.getLoadType())) {
					// enable new data item generation on the load servers side
					V nextBuilder;
					for(final String addr : loadSvcMap.keySet()) {
						nextBuilder = loadSvcMap.get(addr);
						nextBuilder.useNoneItemSrc();
					}
					//
					return getNewItemSrc();
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
					nextBuilder.useNoneItemSrc();
				}
				//
				return getNewItemSrc();
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
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the new data items source");
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
	//
	@Override
	public DataLoadBuilder<T, U> setDataSize(final SizeInBytes dataSize)
	throws IllegalArgumentException, RemoteException {
		this.sizeConfig = dataSize;
		return this;
	}
	//
	@Override
	public DataLoadBuilder<T, U> setDataRanges(final DataRangesConfig rangesConfig)
	throws IllegalArgumentException, RemoteException {
		this.rangesConfig = rangesConfig;
		return this;
	}
}
