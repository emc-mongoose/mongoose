package com.emc.mongoose.load.generator;

import com.emc.mongoose.api.common.exception.OmgShootMyFootException;
import com.github.akurilov.commons.io.Input;
import com.emc.mongoose.api.model.io.task.IoTask;
import com.emc.mongoose.api.model.item.Item;
import com.emc.mongoose.api.model.item.ItemFactory;
import com.emc.mongoose.api.model.item.ItemType;
import com.emc.mongoose.api.model.load.LoadGenerator;
import com.emc.mongoose.api.model.storage.StorageDriver;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;

import com.github.akurilov.concurrent.throttle.IndexThrottle;
import com.github.akurilov.concurrent.throttle.Throttle;

import java.io.IOException;

/**
 Created by andrey on 12.11.16.
 */
public interface LoadGeneratorBuilder<
	I extends Item, O extends IoTask<I>, T extends LoadGenerator<I, O>
> {

	LoadGeneratorBuilder<I, O, T> itemConfig(final ItemConfig itemConfig);

	LoadGeneratorBuilder<I, O, T> loadConfig(final LoadConfig loadConfig);

	LoadGeneratorBuilder<I, O, T> limitConfig(final LimitConfig limitConfig);

	LoadGeneratorBuilder<I, O, T> itemType(final ItemType itemType);

	LoadGeneratorBuilder<I, O, T> itemFactory(final ItemFactory<I> itemFactory);
	
	LoadGeneratorBuilder<I, O, T> authConfig(final AuthConfig authConfig);
	
	LoadGeneratorBuilder<I, O, T> storageDriver(StorageDriver<I, O> storageDriver);
	
	LoadGeneratorBuilder<I, O, T> itemInput(final Input<I> itemInput);

	LoadGeneratorBuilder<I, O, T> originIndex(final int originIndex);

	LoadGeneratorBuilder<I, O, T> rateThrottle(final Throttle rateThrottle);

	LoadGeneratorBuilder<I, O, T> weightThrottle(final IndexThrottle weightThrottle);

	T build()
	throws OmgShootMyFootException, IOException;
}
