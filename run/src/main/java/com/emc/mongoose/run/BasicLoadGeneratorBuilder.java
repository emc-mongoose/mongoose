package com.emc.mongoose.run;

import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.exception.UserShootHisFootException;
import com.emc.mongoose.common.io.ConstantStringInput;
import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.pattern.RangePatternDefinedInput;
import com.emc.mongoose.load.generator.BasicLoadGenerator;
import com.emc.mongoose.model.io.task.BasicIoTaskBuilder;
import com.emc.mongoose.model.io.task.BasicMutableDataIoTaskBuilder;
import com.emc.mongoose.model.io.task.IoTaskBuilder;
import com.emc.mongoose.model.item.BasicItemNameInput;
import com.emc.mongoose.model.item.BasicMutableDataItemFactory;
import com.emc.mongoose.model.item.CsvFileItemInput;
import com.emc.mongoose.model.item.ItemFactory;
import com.emc.mongoose.model.item.ItemNamingType;
import com.emc.mongoose.model.item.ItemType;
import com.emc.mongoose.model.item.NewDataItemInput;
import com.emc.mongoose.model.load.LoadType;
import static com.emc.mongoose.ui.config.Config.ItemConfig.InputConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.LimitConfig;
import com.emc.mongoose.ui.log.LogUtil;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Paths;

/**
 Created by andrey on 12.11.16.
 */
public class BasicLoadGeneratorBuilder<T extends BasicLoadGenerator>
implements LoadGeneratorBuilder<T> {

	private final static Logger LOG = LogManager.getLogger();

	private volatile ItemConfig itemConfig;
	private volatile LoadConfig loadConfig;
	private volatile ItemType itemType;
	private volatile ItemFactory itemFactory;

	@Override
	public BasicLoadGeneratorBuilder<T> setItemConfig(final ItemConfig itemConfig) {
		this.itemConfig = itemConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<T> setLoadConfig(final LoadConfig loadConfig) {
		this.loadConfig = loadConfig;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<T> setItemType(final ItemType itemType) {
		this.itemType = itemType;
		return this;
	}

	@Override
	public BasicLoadGeneratorBuilder<T> setItemFactory(final ItemFactory itemFactory) {
		this.itemFactory = itemFactory;
		return this;
	}

	public T build()
	throws UserShootHisFootException, IOException {

		final LoadType ioType = LoadType.valueOf(loadConfig.getType().toUpperCase());
		final LimitConfig limitConfig = loadConfig.getLimitConfig();

		final Input itemInput;
		final Input<String> dstPathInput;
		final IoTaskBuilder ioTaskBuilder;
		final long countLimit = limitConfig.getCount();
		final int maxQueueSize = loadConfig.getQueueConfig().getSize();
		final boolean isCircular = loadConfig.getCircular();

		final NamingConfig namingConfig = itemConfig.getNamingConfig();
		final ItemNamingType namingType = ItemNamingType.valueOf(
			namingConfig.getType().toUpperCase()
		);
		final String namingPrefix = namingConfig.getPrefix();
		final int namingLength = namingConfig.getLength();
		final int namingRadix = namingConfig.getRadix();
		final long namingOffset = namingConfig.getOffset();
		final InputConfig inputConfig = itemConfig.getInputConfig();

		if(ItemType.PATH.equals(itemType)) {
			// TODO path I/O tasks factory
			ioTaskBuilder = new BasicIoTaskBuilder<>();
		} else {
			ioTaskBuilder = new BasicMutableDataIoTaskBuilder<>()
				.setRangesConfig(itemConfig.getDataConfig().getRangesConfig());
		}
		ioTaskBuilder.setSrcPath(inputConfig.getPath());
		ioTaskBuilder.setIoType(LoadType.valueOf(loadConfig.getType().toUpperCase()));

		final BasicItemNameInput itemNameInput = new BasicItemNameInput(
			namingType, namingPrefix, namingLength, namingRadix, namingOffset
		);
		final String itemInputFile = inputConfig.getFile();
		final String itemInputPath = inputConfig.getPath();

		switch(ioType) {

			case CREATE:

				if(itemInputFile == null || itemInputFile.isEmpty()) {
					if(itemInputPath == null || itemInputPath.isEmpty()) {
						if(itemFactory instanceof BasicMutableDataItemFactory) {
							final SizeInBytes size = itemConfig.getDataConfig().getSize();
							itemInput = new NewDataItemInput(
								itemFactory, itemNameInput, size
							);
						} else {
							itemInput = null; // TODO
						}
					} else {
						// TODO path listing input
						itemInput = null;
					}
				} else {
					try {
						itemInput = new CsvFileItemInput<>(
							Paths.get(itemInputFile), itemFactory
						);
					} catch(final NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}

				final String t = itemConfig.getOutputConfig().getPath();
				if(t == null || t.isEmpty()) {
					dstPathInput = new ConstantStringInput(LogUtil.getDateTimeStamp());
				} else {
					dstPathInput = new RangePatternDefinedInput(t);
				}

				break;

			case READ:
			case UPDATE:
			case DELETE:

				if(itemInputFile == null || itemInputFile.isEmpty()) {
					if(itemInputPath == null || itemInputPath.isEmpty()) {
						throw new UserShootHisFootException(
							"No input (file either path) is specified for non-create generator"
						);
					} else {
						// TODO path listing input
						itemInput = null;
					}
				} else {
					try {
						itemInput = new CsvFileItemInput<>(
							Paths.get(itemInputFile), itemFactory
						);
					} catch(final NoSuchMethodException e) {
						throw new RuntimeException(e);
					}
				}

				dstPathInput = null;

				break;

			default:
				throw new UserShootHisFootException();
		}

		return (T) new BasicLoadGenerator(
			itemInput, dstPathInput, ioTaskBuilder, countLimit, maxQueueSize, isCircular
		);
	}
}
