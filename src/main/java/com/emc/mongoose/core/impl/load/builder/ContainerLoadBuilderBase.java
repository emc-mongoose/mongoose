package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.core.api.container.Container;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.model.ItemSrc;
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.builder.ContainerLoadBuilder;
import com.emc.mongoose.core.api.load.executor.ContainerLoadExecutor;
import com.emc.mongoose.core.impl.data.model.CSVFileItemSrc;
import com.emc.mongoose.core.impl.data.model.NewContainerSrc;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Paths;
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
	public ContainerLoadBuilderBase(final RunTimeConfig rtConfig) {
		super(rtConfig);
	}
	//
	@Override
	public ContainerLoadBuilderBase<T, C, U> setProperties(final RunTimeConfig rtConfig) {
		final String listFilePathStr = rtConfig.getItemSrcFPath();
		if (itemsFileExists(listFilePathStr)) {
			try {
				setItemSrc(
					new CSVFileItemSrc<>(
						Paths.get(listFilePathStr), reqConf.getContainerClass(),
						reqConf.getContentSource()
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
	protected ItemSrc getDefaultItemSource() {
		try {
			if(flagUseNoneItemSrc) {
				return null;
			} else if(flagUseContainerItemSrc && flagUseNewItemSrc) {
				if(IOTask.Type.CREATE.equals(reqConf.getLoadType())) {
					return new NewContainerSrc<>(
						reqConf.getContainerClass()
					);
				}
			} else if(flagUseNewItemSrc) {
				return  new NewContainerSrc<>(
					reqConf.getContainerClass()
				);
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
