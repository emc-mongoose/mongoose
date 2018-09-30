package com.emc.mongoose.load.step.client;

import com.emc.mongoose.logging.Loggers;

import com.github.akurilov.commons.math.MathUtil;
import com.github.akurilov.confuse.Config;
import com.github.akurilov.confuse.impl.BasicConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface ConfigSliceUtil {

	static Config initSlice(final Config config) {
		final Config configSlice = new BasicConfig(config);
		// disable the distributed mode on the slave nodes
		configSlice.val("load-step-node-addrs", Collections.EMPTY_LIST);
		return configSlice;
	}

	static void sliceLongValue(final long configVal, final List<Config> configSlices, final String configPath) {
		final int sliceCount = configSlices.size();
		final long configValPerSlice = (long) Math.ceil(((double) configVal) / sliceCount);
		long remainingVal = configVal;
		for(int i = 0; i < sliceCount; i ++) {
			final Config configSlice = configSlices.get(i);
			if(remainingVal > configValPerSlice) {
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

	static void sliceDoubleValue(final double configVal, final List<Config> configSlices, final String configPath) {
		final int sliceCount = configSlices.size();
		final double configValPerSlice = configVal / sliceCount;
		for(int i = 0; i < sliceCount; i ++) {
			final Config configSlice = configSlices.get(i);
			Loggers.MSG.debug("Config slice #{}: {} = {}", i, configPath, configValPerSlice);
			configSlice.val(configPath, configValPerSlice);
		}
	}

	static void sliceStorageNodeAddrs(final List<Config> configSlices, final List<String> storageNodeAddrs) {
		final int sliceCount = configSlices.size();
		final int storageNodeCount = storageNodeAddrs.size();
		if(storageNodeCount > 1) {
			final int gcd = MathUtil.gcd(sliceCount, storageNodeCount);
			final int sliceCountPerGcd = sliceCount / gcd;
			final int storageNodeCountPerGcd = storageNodeCount / gcd;
			for(int i = 0; i < gcd; i ++) {
				final Collection<String> storageNodeAddrsSlice = new ArrayList<>(storageNodeCountPerGcd);
				for(int j = storageNodeCountPerGcd * i; j < storageNodeCountPerGcd * (i + 1); j ++) {
					storageNodeAddrsSlice.add(storageNodeAddrs.get(j));
				}
				for(int j = i * sliceCountPerGcd; j < (i + 1) * sliceCountPerGcd; j ++) {
					Loggers.MSG.info(
						"Load step slice #{} ({}) storage nodes: {}", j, (j == 0 ? "local" : "remote"),
						Arrays.toString(storageNodeAddrsSlice.toArray())
					);
					configSlices.get(i).val("storage-net-node-addrs", storageNodeAddrsSlice);
				}
			}
		}
	}
}
