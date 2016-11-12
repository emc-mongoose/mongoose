package com.emc.mongoose.run;

import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.load.LoadGenerator;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;

import java.io.IOException;

/**
 Created by andrey on 12.11.16.
 */
public interface LoadGeneratorBuilder<T extends LoadGenerator> {

	LoadGeneratorBuilder<T> setItemConfig(final ItemConfig itemConfig);

	LoadGeneratorBuilder<T> setLoadConfig(final LoadConfig loadConfig);

	LoadGeneratorBuilder<T> setItemType(final ItemType itemType);

	LoadGeneratorBuilder<T> setItemFactory(final ItemFactory itemFactory);

	T build()
	throws UserShootHisFootException, IOException;
}
