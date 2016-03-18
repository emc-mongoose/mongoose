package com.emc.mongoose.run.scenario.engine;
//
import static com.emc.mongoose.common.conf.AppConfig.LoadType;
import static com.emc.mongoose.common.conf.AppConfig.ItemType;
import static com.emc.mongoose.common.conf.AppConfig.StorageType;
import static com.emc.mongoose.common.conf.AppConfig.KEY_ITEM_DATA_SIZE;
import static com.emc.mongoose.common.conf.AppConfig.KEY_LOAD_METRICS_PERIOD;
import static com.emc.mongoose.common.conf.AppConfig.KEY_LOAD_THREADS;
import static com.emc.mongoose.common.conf.AppConfig.KEY_LOAD_TYPE;
import com.emc.mongoose.common.conf.AppConfig;
import com.emc.mongoose.common.conf.BasicConfig;
import com.emc.mongoose.common.conf.SizeInBytes;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
//
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.base.ItemDst;
import com.emc.mongoose.core.api.item.base.ItemSrc;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.load.builder.DataLoadBuilder;
import com.emc.mongoose.core.api.load.builder.LoadBuilder;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.item.ItemTypeUtil;
import com.emc.mongoose.core.impl.item.base.ItemCSVFileDst;
import com.emc.mongoose.core.impl.item.data.ContentSourceBase;
import com.emc.mongoose.util.builder.LoadBuilderFactory;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**
 Created by kurila on 17.03.16.
 */
public class RampupJobContainer
extends SequentialJobContainer {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final LoadBuilder loadJobBuilder;
	private final Class<? extends Item> itemCls;
	private final ContentSource contentSrc;
	private final long limitTime;
	//
	public RampupJobContainer() {
		this(Collections.<String, Object>emptyMap());
	}
	//
	public RampupJobContainer(final Map<String, Object> configTree)
	throws IllegalStateException {
		// disable periodic/intermediate metrics logging
		configTree.put(KEY_LOAD_METRICS_PERIOD, 0);
		// get the default config
		final AppConfig localConfig;
		try {
			localConfig = (AppConfig) BasicConfig.THREAD_CONTEXT.get().clone();
		} catch(final CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
		final ItemType itemType = localConfig.getItemType();
		final StorageType storageType = localConfig.getStorageType();
		itemCls = ItemTypeUtil.getItemClass(itemType, storageType);
		try {
			contentSrc = ContentSourceBase.getInstance(localConfig);
		} catch(final IOException e) {
			throw new IllegalStateException("Failed to init the content source", e);
		}
		limitTime = localConfig.getLoadLimitTime();
		// save the default values being replaced with rampup list values
		final int defaultThreadCount = localConfig.getLoadThreads();
		final SizeInBytes defaultSize = localConfig.getItemDataSize();
		final LoadType defaultLoadType = localConfig.getLoadType();
		localConfig.override(null, configTree);
		// get the lists of the rampup parameters
		final List rawThreadCounts = (List) localConfig.getProperty(KEY_LOAD_THREADS);
		final List rawSizes = (List) localConfig.getProperty(KEY_ITEM_DATA_SIZE);
		final List rawLoadTypes = (List) localConfig.getProperty(KEY_LOAD_TYPE);
		// return the default values replaced with the list values back
		if(defaultThreadCount > 0) {
			localConfig.setProperty(KEY_LOAD_THREADS, defaultThreadCount);
		}
		if(defaultSize != null) {
			localConfig.setProperty(KEY_ITEM_DATA_SIZE, defaultSize.toString());
		}
		if(defaultLoadType != null) {
			localConfig.setProperty(KEY_LOAD_TYPE, defaultLoadType.name().toLowerCase());
		}
		//
		try {
			loadJobBuilder = LoadBuilderFactory.getInstance(localConfig);
		} catch(final ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException("Failed to init the rampup job", e);
		} catch(final InvocationTargetException e) {
			throw new IllegalStateException("Failed to init the rampup job", e.getTargetException());
		} catch(final Throwable t) {
			t.printStackTrace(System.out);
			throw t;
		}
		//
		initForEach(rawThreadCounts, rawSizes, rawLoadTypes);
	}
	//
	private void initForEach(
		final List rawThreadCounts, final List rawSizes, final List rawLoadTypes
	) {
		int nextThreadCount;
		for(final Object rawThreadCount : rawThreadCounts) {
			//
			nextThreadCount = 0;
			if(rawThreadCount instanceof Integer) {
				nextThreadCount = (Integer) rawThreadCount;
			} else if(rawThreadCount instanceof String) {
				try {
					nextThreadCount = Integer.parseInt((String) rawThreadCount);
				} catch(final NumberFormatException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to parse the number \"{}\"", rawThreadCount
					);
				}
			} else {
				LOG.warn(Markers.ERR, "Invalid thread count type: \"{}\"", rawThreadCount);
			}
			if(nextThreadCount <= 0) {
				LOG.warn(
					Markers.ERR, "Invalid thread count value \"{}\", skipping", nextThreadCount
				);
				continue;
			}
			//
			initForThreadCount(nextThreadCount, rawSizes, rawLoadTypes);
		}
	}
	//
	private void initForThreadCount(
		final int nextThreadCount, final List rawSizes, final List rawLoadTypes
	) {
		if(rawSizes == null || rawSizes.size() == 0) {
			initForThreadCountAndSize(nextThreadCount, null, rawLoadTypes);
		} else {
			SizeInBytes nextSize;
			for(final Object rawSize : rawSizes) {
				nextSize = null;
				if(rawSize instanceof Integer) {
					final int s = (Integer) rawSize;
					nextSize = new SizeInBytes(s, s, 0);
				} else if(rawSize instanceof Long) {
					final long s = (Long) rawSize;
					nextSize = new SizeInBytes(s, s, 0);
				} else if(rawSize instanceof String) {
					try {
						nextSize = new SizeInBytes((String) rawSize);
					} catch(final IllegalArgumentException e) {
						LogUtil.exception(
							LOG, Level.WARN, e, "Failed to parse the size \"{}\"", rawSize
						);
					}
				} else {
					LOG.warn(Markers.ERR, "Invalid size type: \"{}\"", rawSize);
				}
				if(nextSize == null) {
					LOG.warn(Markers.ERR, "Invalid size value \"{}\", skipping the step", rawSize);
					continue;
				}
				//
				initForThreadCountAndSize(nextThreadCount, nextSize, rawLoadTypes);
			}
		}
	}
	//
	private void initForThreadCountAndSize(
		final int nextThreadCount, final SizeInBytes nextSize, final List rawLoadTypes
	) {
		LoadType nextLoadType;
		ItemDst prevLoadJobItemDst = null;
		for(final Object rawLoadType : rawLoadTypes) {
			nextLoadType = null;
			if(rawLoadType instanceof LoadType) {
				nextLoadType = (LoadType) rawLoadType;
			} else if(rawLoadType instanceof String) {
				try {
					nextLoadType = LoadType.valueOf(((String) rawLoadType).toUpperCase());
				} catch(final IllegalArgumentException e) {
					LogUtil.exception(
						LOG, Level.WARN, e, "Failed to parse the load type \"{}\"",
						rawLoadType
					);
				}
			} else {
				LOG.warn(Markers.ERR, "Invalid load type: \"{}\"", rawLoadType);
			}
			if(nextLoadType == null) {
				LOG.warn(
					Markers.ERR, "Invalid load type \"{}\", skipping the step", rawLoadType
				);
				continue;
			}
			//
			try {
				prevLoadJobItemDst = append(
					prevLoadJobItemDst == null ? null : prevLoadJobItemDst.getItemSrc(),
					nextThreadCount, nextSize, nextLoadType
				);
			} catch(final IOException e) {
				LogUtil.exception(
					LOG, Level.ERROR, e, "Failed to build the load job \"{} x {} x {}\"",
					nextLoadType.name(), nextThreadCount, nextSize.toString()
				);
			}
		}
	}
	//
	private LoadExecutor append(
		final ItemSrc itemSrc,
		final int nextThreadCount, final SizeInBytes nextSize, final LoadType nextLoadType
	) throws IOException {
		//
		final LoadExecutor nextLoadJob;
		//
		loadJobBuilder
			.setLoadType(nextLoadType)
			.setThreadCount(nextThreadCount)
			.setItemSrc(itemSrc)
			.setItemDst(new ItemCSVFileDst<>(itemCls, contentSrc));
		if(loadJobBuilder instanceof DataLoadBuilder && nextSize != null) {
			((DataLoadBuilder) loadJobBuilder).setDataSize(nextSize);
		}
		nextLoadJob = loadJobBuilder.build();
		//
		if(nextLoadJob != null) {
			append(new SingleJobContainer(nextLoadJob, limitTime));
		}
		//
		return nextLoadJob;
	}
}
