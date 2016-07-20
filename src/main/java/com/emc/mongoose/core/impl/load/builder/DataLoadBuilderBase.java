package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.DataRangesConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.data.FileDataItemInput;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.core.impl.item.base.CsvFileItemOutput;
import com.emc.mongoose.core.impl.item.data.CsvFileDataItemInput;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;
/**
 Created by kurila on 20.10.15.
 */
public abstract class DataLoadBuilderBase<T extends DataItem, U extends LoadExecutor<T>>
extends LoadBuilderBase<T, U>
implements DataLoadBuilder<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	protected SizeInBytes sizeConfig;
	protected DataRangesConfig rangesConfig;
	//
	public DataLoadBuilderBase(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	public DataLoadBuilderBase<T, U> clone()
	throws CloneNotSupportedException {
		final DataLoadBuilderBase<T, U> lb = (DataLoadBuilderBase<T, U>) super.clone();
		lb.sizeConfig = sizeConfig;
		lb.rangesConfig = rangesConfig;
		return lb;
	}
	//
	@Override
	protected Input<T> getNewItemInput(final IoConfig<T, ?> ioConfigCopy)
	throws NoSuchMethodException {
		final ItemNamingType namingType = appConfig.getItemNamingType();
		return ioConfigCopy.getNewDataItemsInput(
			namingType, ioConfigCopy.getItemClass(), sizeConfig
		);
	}
	//
	@Override
	public String toString() {
		return super.toString() + "x" + sizeConfig.toString();
	}
	//
	@Override
	public DataLoadBuilder<T, U> setAppConfig(final AppConfig appConfig)
	throws IllegalStateException, RemoteException {
		super.setAppConfig(appConfig);
		setDataSize(appConfig.getItemDataSize());
		setDataRanges(appConfig.getItemDataRanges());
		//
		final String listFilePathStr = appConfig.getItemSrcFile();
		if(itemsFileExists(listFilePathStr)) {
			try {
				setInput(
					new CsvFileDataItemInput<>(
						Paths.get(listFilePathStr), (Class<T>) ioConfig.getItemClass(),
						ioConfig.getContentSource()
					)
				);
			} catch(final IOException | NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
			}
		}
		//
		final String dstFilePathStr = appConfig.getItemDstFile();
		if(dstFilePathStr != null && !dstFilePathStr.isEmpty()) {
			final Path dstFilePath = Paths.get(dstFilePathStr);
			try {
				/*if(Files.exists(dstFilePath) && Files.size(dstFilePath) > 0) {
					LOG.warn(
						Markers.ERR, "Items destination file \"{}\" is not empty", dstFilePathStr
					);
				}*/
				setOutput(
					new CsvFileItemOutput<>(
						dstFilePath, (Class<T>) ioConfig.getItemClass(), ioConfig.getContentSource()
					)
				);
			} catch(final IOException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file output");
			}
		}
		//
		return this;
	}
	//
	@Override
	public LoadBuilder<T, U> setInput(final Input<T> itemInput)
	throws RemoteException {
		super.setInput(itemInput);
		if(itemInput instanceof FileDataItemInput) {
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
	@Override
	public DataLoadBuilder<T, U> setDataSize(final SizeInBytes dataSize)
	throws IllegalArgumentException {
		LOG.debug(Markers.MSG, "Set data item size: {}", dataSize.toString());
		this.sizeConfig = dataSize;
		return this;
	}
	@Override
	public DataLoadBuilder<T, U> setDataRanges(final DataRangesConfig rangesConfig) {
		LOG.debug(
			Markers.MSG, "Set fixed byte ranges: {}",
			rangesConfig == null ? null : rangesConfig.toString()
		);
		this.rangesConfig = rangesConfig;
		return this;
	}
}
