package com.emc.mongoose.base.load.step.client;

import com.emc.mongoose.base.item.naming.ItemNameInput.ItemNamingType;
import com.emc.mongoose.base.logging.LogUtil;
import com.emc.mongoose.base.logging.Loggers;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.math.MathUtil;
import com.github.akurilov.commons.reflection.TypeUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;
import org.apache.logging.log4j.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import static com.emc.mongoose.base.config.el.Language.withLanguage;
import static com.emc.mongoose.base.item.naming.ItemNameInput.ItemNamingType.SERIAL;

public interface ConfigSliceUtil {

	static Config initSlice(final Config config) {
		final Config configSlice = new BasicConfig(config);
		// disable the distributed mode on the slave nodes
		configSlice.val("load-step-node-addrs", Collections.EMPTY_LIST);
		return configSlice;
	}

	static void sliceLongValue(
					final long configVal, final List<Config> configSlices, final String configPath) {
		final var sliceCount = configSlices.size();
		final var configValPerSlice = (long) Math.ceil(((double) configVal) / sliceCount);
		var remainingVal = configVal;
		for (var i = 0; i < sliceCount; i++) {
			final var configSlice = configSlices.get(i);
			if (remainingVal > configValPerSlice) {
				Loggers.MSG.debug("Config slice #{}: {} = {}", i, configPath, configValPerSlice);
				configSlice.val(configPath, configValPerSlice);
				remainingVal -= configValPerSlice;
			} else {
				Loggers.MSG.debug("Config slice #{}: {} = {}", i, configPath, remainingVal);
				configSlice.val(configPath, remainingVal);
				remainingVal = 0;
			}
		}
	}

	static void sliceDoubleValue(
					final double configVal, final List<Config> configSlices, final String configPath) {
		final var sliceCount = configSlices.size();
		final var configValPerSlice = configVal / sliceCount;
		for (var i = 0; i < sliceCount; i++) {
			final var configSlice = configSlices.get(i);
			Loggers.MSG.debug("Config slice #{}: {} = {}", i, configPath, configValPerSlice);
			configSlice.val(configPath, configValPerSlice);
		}
	}

	static void sliceStorageNodeAddrs(
					final List<Config> configSlices, final List<String> storageNodeAddrs) {
		final var sliceCount = configSlices.size();
		final var storageNodeCount = storageNodeAddrs.size();
		if (storageNodeCount > 1) {
			final var gcd = MathUtil.gcd(sliceCount, storageNodeCount);
			final var sliceCountPerGcd = sliceCount / gcd;
			final var storageNodeCountPerGcd = storageNodeCount / gcd;
			for (var i = 0; i < gcd; i++) {
				final var storageNodeAddrsSlice = (Collection<String>) new ArrayList<String>(storageNodeCountPerGcd);
				for (var j = storageNodeCountPerGcd * i; j < storageNodeCountPerGcd * (i + 1); j++) {
					storageNodeAddrsSlice.add(storageNodeAddrs.get(j));
				}
				for (var j = i * sliceCountPerGcd; j < (i + 1) * sliceCountPerGcd; j++) {
					Loggers.MSG.info(
									"Load step slice #{} ({}) storage nodes: {}",
									j,
									(j == 0 ? "local" : "remote"),
									Arrays.toString(storageNodeAddrsSlice.toArray()));
					configSlices.get(i).val("storage-net-node-addrs", storageNodeAddrsSlice);
				}
			}
		}
	}

	static void sliceItemNaming(final List<Config> configSlices) {
		try {
			final var namingConfig = configSlices.get(0).configVal("item-naming");
			final var namingType = ItemNamingType.valueOf(namingConfig.stringVal("type").toUpperCase());
			if (SERIAL.equals(namingType)) {
				final var sliceCount = configSlices.size();
				final var srcNamingSeedRaw = namingConfig.val("seed");
				long srcNamingSeed;
				try {
					srcNamingSeed = TypeUtil.typeConvert(srcNamingSeedRaw, long.class);
				} catch (final ClassCastException | NumberFormatException e) {
					if (srcNamingSeedRaw instanceof String) {
						try (
										final var in = withLanguage(ExpressionInput.builder())
														.expression((String) srcNamingSeedRaw)
														.<ExpressionInput<Long>> build()) {
							srcNamingSeed = in.get();
						}
					} else {
						throw new Exception(
										"Item naming seed value (" + srcNamingSeedRaw + ") should be an integer either expression");
					}
				}
				final var srcNamingStep = namingConfig.intVal("step");
				for (var i = 0; i < sliceCount; i++) {
					final var configSlice = configSlices.get(i);
					Loggers.MSG.debug(
									"Item naming slicing: slice #{}, seed: {}, step: {}", i, srcNamingSeed + i * srcNamingStep,
									srcNamingStep * sliceCount);
					configSlice.val("item-naming-seed", srcNamingSeed + i * srcNamingStep);
					configSlice.val("item-naming-step", srcNamingStep * sliceCount);
				}
			}
		} catch (final NoSuchElementException ignored) {} catch (final Exception e) {
			LogUtil.exception(Level.ERROR, e, "Item naming slicing failure");
		}
	}
}
