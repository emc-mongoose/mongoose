package com.emc.mongoose.core.impl.load.generator;
//
import com.emc.mongoose.common.conf.enums.LoadType;
import com.emc.mongoose.common.io.Input;
//
import com.emc.mongoose.core.api.io.conf.IoConfig;
import com.emc.mongoose.core.api.io.task.IoTask;
import com.emc.mongoose.core.api.item.base.Item;
import com.emc.mongoose.core.api.item.container.Container;
import com.emc.mongoose.core.api.item.data.ContentSource;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.api.load.generator.IoTaskGenerator;
/**
 Created by andrey on 08.04.16.
 */
public abstract class IoTaskGeneratorBase<T extends Item, A extends IoTask<T>>
extends BasicItemGenerator<T>
implements IoTaskGenerator<T, A> {
	//
	protected final IoConfig<? extends Item, ? extends Container<? extends Item>> ioConfigCopy;
	protected final LoadType loadType;
	protected final ContentSource dataSrc;
	protected final LoadExecutor<T> loadExecutor;
	//
	protected IoTaskGeneratorBase(
		// BasicItemGenerator args
		final Input<T> itemInput, final long maxCount, final int batchSize,
		final boolean isCircular, final boolean isShuffling, final int maxItemQueueSize,
		final float rateLimit,
		// BasicIoTaskGenerator args
		final IoConfig<? extends Item, ? extends Container<? extends Item>> ioConfig,
		final LoadExecutor<T> loadExecutor
	) {
		super(itemInput, maxCount, batchSize, isCircular, isShuffling, maxItemQueueSize, rateLimit);
		//
		try {
			this.ioConfigCopy = ioConfig.clone();
		} catch(final CloneNotSupportedException e) {
			throw new IllegalStateException(e);
		}
		loadType = ioConfig.getLoadType();
		dataSrc = ioConfig.getContentSource();
		this.loadExecutor = loadExecutor;
	}
	//
	@Override
	public final IoConfig<? extends Item, ? extends Container<? extends Item>> getIoConfig() {
		return ioConfigCopy;
	}
	//
	@Override
	public final LoadType getLoadType() {
		return loadType;
	}
	//
	@Override
	public final LoadExecutor<T> getLoadExecutor() {
		return loadExecutor;
	}
	//
	protected A getIoTask(final T item);
	//
	protected int getIoTasks()
}
