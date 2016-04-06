package com.emc.mongoose.client.impl.load.builder;
//
import com.emc.mongoose.client.api.load.builder.ContainerLoadBuilderClient;
import com.emc.mongoose.client.api.load.executor.ContainerLoadClient;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.enums.ItemNamingType;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
//
import com.emc.mongoose.core.impl.item.base.BasicItemNameInput;
import com.emc.mongoose.core.impl.item.base.ItemCsvFileOutput;
import com.emc.mongoose.core.impl.item.base.ItemCSVFileSrc;
import com.emc.mongoose.core.impl.item.data.NewContainerSrc;
import com.emc.mongoose.server.api.load.builder.ContainerLoadBuilderSvc;
import com.emc.mongoose.server.api.load.executor.ContainerLoadSvc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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
				setInput(
					new ItemCSVFileSrc<>(
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
	protected ItemSrc<C> getNewItemSrc()
	throws NoSuchMethodException {
		ItemNamingType namingType = appConfig.getItemNamingType();
		final Class<C> containerClass = (Class<C>) ioConfig.getContainerClass();
		return new NewContainerSrc<>(
			containerClass,
			new BasicItemNameInput(
				namingType,
				appConfig.getItemNamingPrefix(), appConfig.getItemNamingLength(),
				appConfig.getItemNamingRadix(), appConfig.getItemNamingOffset()
			)
		);
	}
}
