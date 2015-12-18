package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.item.base.ItemNamingScheme;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.DataItem;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.ContainerLoadBuilder;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
import com.emc.mongoose.core.impl.item.base.BasicItemNamingScheme;
import com.emc.mongoose.core.impl.item.base.ItemCSVFileSrc;
import com.emc.mongoose.core.impl.item.data.NewContainerSrc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.Arrays;
//
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
	public ContainerLoadBuilderBase(final RunTimeConfig rtConfig)
	throws RemoteException {
		super(rtConfig);
	}
	//
	@Override
	public ContainerLoadBuilderBase<T, C, U> setRunTimeConfig(final RunTimeConfig rtConfig)
	throws RemoteException {
		super.setRunTimeConfig(rtConfig);
		//
		final String listFilePathStr = rtConfig.getItemSrcFile();
		if(itemsFileExists(listFilePathStr)) {
			try {
				setItemSrc(
					new ItemCSVFileSrc<>(
						Paths.get(listFilePathStr), (Class<C>) ioConfig.getContainerClass(),
						ioConfig.getContentSource()
					)
				);
			} catch(final IOException | NoSuchMethodException e) {
				LogUtil.exception(LOG, Level.ERROR, e, "Failed to use CSV file input");
			}
		}
		return this;
	}
	//
	@SuppressWarnings("unchecked")
	private ItemSrc getNewItemSrc()
	throws NoSuchMethodException {
		final String ns = rtConfig.getItemNaming();
		ItemNamingScheme.Type namingSchemeType = ItemNamingScheme.Type.RANDOM;
		if(ns != null && !ns.isEmpty()) {
			try {
				namingSchemeType = ItemNamingScheme.Type.valueOf(ns);
			} catch(final IllegalArgumentException e) {
				LogUtil.exception(
					LOG, Level.WARN, e,
					"Failed to parse the naming scheme \"{}\", acceptable values are: {}",
					ns, Arrays.toString(ItemNamingScheme.Type.values())
				);
			}
		}
		return new NewContainerSrc<>(
			ioConfig.getContainerClass(), new BasicItemNamingScheme(namingSchemeType)
		);
	}
	//
	@SuppressWarnings("unchecked")
	protected ItemSrc getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.CREATE.equals(ioConfig.getLoadType())) {
					getNewItemSrc();
				}
			} else if(flagUseNewItemSrc) {
				return getNewItemSrc();
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
