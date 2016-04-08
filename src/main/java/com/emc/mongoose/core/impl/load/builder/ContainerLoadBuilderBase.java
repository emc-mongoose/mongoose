package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.builder.ContainerLoadBuilder;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
import com.emc.mongoose.core.impl.item.base.BasicItemNameInput;
//
import com.emc.mongoose.core.impl.item.base.ItemCsvFileOutput;
import com.emc.mongoose.core.impl.item.base.CsvFileItemInput;
import com.emc.mongoose.core.impl.item.data.NewContainerInput;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
/**
 * Created by gusakk on 21.10.15.
 */
public abstract class ContainerLoadBuilderBase<
	T extends DataItem,
	C extends Container<T>,
	U extends ContainerLoadExecutor<T, C>
>
extends LoadBuilderBase<C, U>
implements ContainerLoadBuilder<T, C, U>{
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected boolean flagUseContainerItemSrc;
	//
	public ContainerLoadBuilderBase(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	public ContainerLoadBuilderBase<T, C, U> setAppConfig(final AppConfig appConfig)
	throws RemoteException {
		super.setAppConfig(appConfig);
		//
		final String listFilePathStr = appConfig.getItemSrcFile();
		if(itemsFileExists(listFilePathStr)) {
			try {
				setInput(
					new CsvFileItemInput<>(
						Paths.get(listFilePathStr), (Class<C>) ioConfig.getContainerClass(),
						ioConfig.getContentSource()
					)
				);
			} catch(final IOException | NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
			}
		}
		//
		final String dstFilePath = appConfig.getItemDstFile();
		if(dstFilePath != null && !dstFilePath.isEmpty()) {
			try {
				setOutput(
					new ItemCsvFileOutput<>(
						Paths.get(dstFilePath), (Class<C>) ioConfig.getContainerClass(),
						ioConfig.getContentSource()
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
	@Override @SuppressWarnings("unchecked")
	protected Input<C> getNewItemInput()
	throws NoSuchMethodException {
		ItemNamingType namingType = appConfig.getItemNamingType();
		final Class<C> containerClass = (Class<C>) ioConfig.getContainerClass();
		return new NewContainerInput<>(
			containerClass,
			new BasicItemNameInput(
				namingType,
				appConfig.getItemNamingPrefix(), appConfig.getItemNamingLength(),
				appConfig.getItemNamingRadix(), appConfig.getItemNamingOffset()
			)
		);
	}
	//
	@Override
	protected Input<C> getDefaultItemInput() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(LoadType.WRITE.equals(ioConfig.getLoadType())) {
					getNewItemInput();
				}
			} else if(flagUseNewItemSrc) {
				return getNewItemInput();
			}
		} catch(final NoSuchMethodException e) {
			LogUtil.exception(LOG, Level.ERROR, e, "Failed to build the new data items source");
		}
		return null;
	}
	//
	@Override
	public ContainerLoadBuilderBase<T, C, U> clone()
	throws CloneNotSupportedException {
		return (ContainerLoadBuilderBase<T, C, U>) super.clone();
	}
}
