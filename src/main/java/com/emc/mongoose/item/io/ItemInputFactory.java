package com.emc.mongoose.item.io;

import com.emc.mongoose.item.Item;
import com.emc.mongoose.item.ItemFactory;
import com.emc.mongoose.item.ItemType;
import com.emc.mongoose.item.op.Operation;
import com.emc.mongoose.logging.LogUtil;
import com.emc.mongoose.logging.Loggers;
import com.emc.mongoose.storage.driver.StorageDriver;

import com.github.akurilov.commons.io.Input;
import com.github.akurilov.commons.io.file.BinFileInput;

import com.github.akurilov.confuse.Config;

import org.apache.logging.log4j.Level;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public interface ItemInputFactory {

	static <I extends Item, O extends Operation<I>> Input<I> createItemInput(
		final Config itemConfig, final StorageDriver<I, O> storageDriver, final int batchSize
	) {
		Input<I> itemInput = null;

		final ItemType itemType = ItemType.valueOf(itemConfig.stringVal("type").toUpperCase());
		final ItemFactory<I> itemFactory = ItemType.getItemFactory(itemType);
		final Config itemInputConfig = itemConfig.configVal("input");
		final String itemInputFile = itemInputConfig.stringVal("file");

		if(itemInputFile != null && !itemInputFile.isEmpty()) {
			itemInput = createFileItemInput(itemFactory, itemInputFile);
			Loggers.MSG.debug("Using the file \"{}\" as items input", itemInputFile);
		} else {
			final String itemInputPath = itemInputConfig.stringVal("path");
			if(itemInputPath != null && !itemInputPath.isEmpty()) {
				itemInput = createPathItemInput(itemConfig, storageDriver, batchSize, itemFactory, itemInputPath);
				Loggers.MSG.debug("Using the storage path \"{}\" as items input", itemInputPath);
			}
		}

		return itemInput;
	}

	static <I extends Item> Input<I> createFileItemInput(final ItemFactory<I> itemFactory, final String itemInputFile) {

		Input<I> fileItemInput = null;

		final Path itemInputFilePath = Paths.get(itemInputFile);
		try {
			if(itemInputFile.endsWith(".csv")) {
				try {
					fileItemInput = new CsvFileItemInput<>(itemInputFilePath, itemFactory);
				} catch(final NoSuchMethodException e) {
					throw new AssertionError(e);
				}
			} else {
				fileItemInput = new BinFileInput<>(itemInputFilePath);
			}
		} catch(final IOException e) {
			LogUtil.exception(Level.WARN, e, "Failed to open the item input file \"{}\"", itemInputFile);
		}

		return fileItemInput;
	}

	static <I extends Item, O extends Operation<I>> Input<I> createPathItemInput(
		final Config itemConfig, final StorageDriver<I, O> storageDriver, final int batchSize,
		final ItemFactory<I> itemFactory, final String itemInputPath
	) {
		Input<I> itemInput = null;
		try {
			final Config namingConfig = itemConfig.configVal("naming");
			final String namingPrefix = namingConfig.stringVal("prefix");
			final int namingRadix = namingConfig.intVal("radix");
			itemInput = new StorageItemInput<>(
				storageDriver, batchSize, itemFactory, itemInputPath, namingPrefix, namingRadix
			);
		} catch(final IllegalStateException | IllegalArgumentException e) {
			LogUtil.exception(Level.WARN, e, "Failed to initialize the data input");
		}
		return itemInput;
	}
}
