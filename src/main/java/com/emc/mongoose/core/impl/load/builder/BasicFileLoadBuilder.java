package com.emc.mongoose.core.impl.load.builder;
//
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.log.LogUtil;
//
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.container.Directory;
import com.emc.mongoose.core.api.item.data.FileItem;
import com.emc.mongoose.core.api.io.conf.FileIOConfig;
import com.emc.mongoose.core.api.load.executor.FileLoadExecutor;
//
import com.emc.mongoose.core.impl.io.conf.BasicFileIOConfig;
import com.emc.mongoose.core.impl.load.executor.BasicFileLoadExecutor;
import com.emc.mongoose.core.impl.load.executor.BasicMixedFileLoadExecutor;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 26.11.15.
 */
public class BasicFileLoadBuilder<T extends FileItem, U extends FileLoadExecutor<T>>
extends DataLoadBuilderBase<T, U> {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	public BasicFileLoadBuilder(final AppConfig appConfig)
	throws RemoteException {
		super(appConfig);
	}
	//
	@Override
	protected FileIOConfig<T, ? extends Directory<T>> getDefaultIoConfig() {
		return new BasicFileIOConfig<>();
	}
	//
	@Override
	public void invokePreConditions()
	throws IllegalStateException {
		// create parent directories
		final Container c = ioConfig.getContainer();
		final String parentDirectories = c == null ? null : c.getName();
		if(parentDirectories != null && !parentDirectories.isEmpty()) {
			try {
				Files.createDirectories(Paths.get(parentDirectories));
			} catch(final IOException e) {
				throw new IllegalStateException(
					"Failed to create target directories @ \"" + parentDirectories + "\""
				);
			}
		}
	}
	//
	@Override @SuppressWarnings("unchecked")
	protected U buildActually() {
		final LoadType loadType = ioConfig.getLoadType();
		if(LoadType.MIXED.equals(loadType)) {
			final Map<LoadType, Integer> loadTypeWeightMap = LoadType.getMixedLoadWeights(
				(List<String>) appConfig.getProperty(AppConfig.KEY_LOAD_TYPE)
			);
			final Map<LoadType, ItemSrc<T>> itemSrcMap = new HashMap<>();
			for(final LoadType nextLoadType : loadTypeWeightMap.keySet()) {
				try {
					itemSrcMap.put(
						nextLoadType,
						LoadType.WRITE.equals(nextLoadType) ? getNewItemSrc() : itemSrc
					);
				} catch(final NoSuchMethodException e) {
					LogUtil.exception(LOG, Level.ERROR, e, "Failed to build new item src");
				}
			}
			return (U) new BasicMixedFileLoadExecutor<>(
				appConfig, (FileIOConfig<T, ? extends Directory<T>>) ioConfig, threadCount,
				maxCount, rateLimit, sizeConfig, rangesConfig, loadTypeWeightMap, itemSrcMap
			);
		} else {
			return (U) new BasicFileLoadExecutor<>(
				appConfig, (FileIOConfig<T, ? extends Directory<T>>) ioConfig,
				threadCount, itemSrc == null ? getDefaultItemSrc() : itemSrc, maxCount, rateLimit,
				sizeConfig, rangesConfig
			);
		}
	}
}
