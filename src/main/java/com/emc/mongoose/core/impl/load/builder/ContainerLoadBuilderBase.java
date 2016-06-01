package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.load.builder.ContainerLoadBuilder;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
//
import com.emc.mongoose.core.impl.item.base.CsvFileItemOutput;
import com.emc.mongoose.core.impl.item.base.CsvFileItemInput;
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
		final String dstFilePathStr = appConfig.getItemDstFile();
		if(dstFilePathStr != null && !dstFilePathStr.isEmpty()) {
			final Path dstFilePath = Paths.get(dstFilePathStr);
			try {
				if(Files.exists(dstFilePath) && Files.size(dstFilePath) > 0) {
					LOG.warn(
						Markers.ERR, "Items destination file \"{}\" is not empty", dstFilePathStr
					);
				}
				setOutput(
					new CsvFileItemOutput<>(
						dstFilePath, (Class<C>) ioConfig.getContainerClass(),
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
	@Override
	protected Input<C> getNewItemInput(final IoConfig<C, ?> ioConfigCopy)
	throws NoSuchMethodException {
		final ItemNamingType namingType = appConfig.getItemNamingType();
		return (Input<C>) ioConfigCopy.getNewContainersInput(
			namingType, (Class) ioConfigCopy.getContainerClass()
		);
	}
	//
	@Override
	public ContainerLoadBuilderBase<T, C, U> clone()
	throws CloneNotSupportedException {
		return (ContainerLoadBuilderBase<T, C, U>) super.clone();
	}
}
