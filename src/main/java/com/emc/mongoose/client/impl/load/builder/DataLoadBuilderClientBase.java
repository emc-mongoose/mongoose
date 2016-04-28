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
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.FileDataItemInput;
import com.emc.mongoose.core.api.io.conf.IoConfig;
//
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.impl.item.base.BasicItemNameInput;
import com.emc.mongoose.core.impl.item.data.NewDataItemInput;
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
	public DataLoadBuilderClient<T, W, U> setInput(final Input<T> itemInput)
	throws RemoteException {
		super.setInput(itemInput);
		//
		if(itemInput instanceof FileDataItemInput) {
			// calculate approx average data item size
			final FileDataItemInput<T> fileInput = (FileDataItemInput<T>) itemInput;
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
	protected Input<T> getNewItemInput()
	throws NoSuchMethodException {
		final ItemNamingType namingType = appConfig.getItemNamingType();
		final BasicItemNameInput bing = new BasicItemNameInput(
			namingType,
			appConfig.getItemNamingPrefix(), appConfig.getItemNamingLength(),
			appConfig.getItemNamingRadix(), appConfig.getItemNamingOffset()
		);
		return new NewDataItemInput<>(
			(Class<T>) ioConfig.getItemClass(), bing, ioConfig.getContentSource(), sizeConfig
		);
	}
	//
	@Override
	protected final void resetItemInput() {
		super.resetItemInput();
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
