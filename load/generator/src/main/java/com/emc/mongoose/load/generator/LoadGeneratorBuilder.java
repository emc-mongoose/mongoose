package com.emc.mongoose.load.generator;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.io.task.IoTask;
import static com.emc.mongoose.model.io.task.IoTask.IoResult;
import com.emc.mongoose.model.item.Item;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import com.emc.mongoose.model.storage.StorageDriver;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;

import java.io.IOException;
import java.util.List;

/**
 Created by andrey on 12.11.16.
 */
public interface LoadGeneratorBuilder<
	I extends Item, O extends IoTask<I, R>, R extends IoResult, T extends LoadGenerator<I, O, R>
> {

	LoadGeneratorBuilder<I, O, R, T> setItemConfig(final ItemConfig itemConfig);

	LoadGeneratorBuilder<I, O, R, T> setLoadConfig(final LoadConfig loadConfig);

	LoadGeneratorBuilder<I, O, R, T> setItemType(final ItemType itemType);

	LoadGeneratorBuilder<I, O, R, T> setItemFactory(final ItemFactory<I> itemFactory);

	LoadGeneratorBuilder<I, O, R, T> setStorageDrivers(
		final List<StorageDriver<I, O, R>> storageDrivers
	);

	T build()
	throws UserShootHisFootException, IOException;
}
