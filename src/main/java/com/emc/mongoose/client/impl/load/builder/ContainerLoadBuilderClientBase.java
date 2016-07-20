package com.emc.mongoose.client.impl.load.builder;

import com.emc.mongoose.client.api.load.builder.ContainerLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.ContainerLoadClient;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.impl.item.base.CsvFileItemOutput;
import com.emc.mongoose.core.impl.item.base.CsvFileItemInput;
import com.emc.mongoose.server.api.load.builder.ContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.ContainerLoadSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.RemoteException;

/**
 Created by kurila on 21.10.15.
 */
public abstract class ContainerLoadBuilderClientBase<
	T extends DataItem,
	C extends Container<T>,
	W extends ContainerLoadSvc<T, C>,
	U extends ContainerLoadClient<T, C, W>,
	V extends ContainerLoadBuilderSvc<T, C, W>
> extends LoadBuilderClientBase<C, W, U, V>
implements ContainerLoadBuilderClient<T, C, W, U> {
	//
	private static final Logger LOG = LogManager.getLogger();
	//
	protected ContainerLoadBuilderClientBase()
	throws IOException {
		this(BasicConfig.THREAD_CONTEXT.get());
	}
	//
	protected ContainerLoadBuilderClientBase(final AppConfig appConfig)
	throws IOException {
		super(appConfig);
	}
	//
	@Override @SuppressWarnings("unchecked")
	public ContainerLoadBuilderClientBase<T, C, W, U, V> setAppConfig(final AppConfig appConfig)
	throws RemoteException {
		super.setAppConfig(appConfig);
		//
		final String listFilePathStr = appConfig.getItemSrcFile();
		if(itemsFileExists(listFilePathStr)) {
			try {
				final Input<C> in = new CsvFileItemInput<>(
					Paths.get(listFilePathStr), (Class<C>) ioConfig.getContainerClass(),
					ioConfig.getContentSource()
				);
				setInput(in);
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
	@Override @SuppressWarnings("unchecked")
	protected Input<C> getNewItemInput(final IoConfig<C, ?> ioConfigCopy)
	throws NoSuchMethodException {
		final ItemNamingType namingType = appConfig.getItemNamingType();
		return (Input<C>) ioConfigCopy.getNewContainersInput(
			namingType, (Class) ioConfigCopy.getContainerClass()
		);
	}
}
