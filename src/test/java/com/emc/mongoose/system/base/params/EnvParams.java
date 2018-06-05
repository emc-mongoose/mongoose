package com.emc.mongoose.system.base.params;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class EnvParams {

	public final static List<Object[]> PARAMS = new ArrayList<>();

	static {

		final Map<String, String> env = System.getenv();

		final List<StorageType> includedStorageTypes = new ArrayList<>();
		final String storageTypeValues = env.get(StorageType.KEY_ENV);
		for(final String storageTypeValue : storageTypeValues.split(",")) {
			includedStorageTypes.add(StorageType.valueOf(storageTypeValue.toUpperCase()));
		}

		final List<NodeCount> includedNodeCounts = new ArrayList<>();
		final String nodeCountValues = env.get(NodeCount.KEY_ENV);
		for(final String nodeCountValue : nodeCountValues.split(",")) {
			includedNodeCounts.add(NodeCount.valueOf(nodeCountValue.toUpperCase()));
		}

		final List<Concurrency> includedConcurrencies = new ArrayList<>();
		final String concurrencyValues = env.get(Concurrency.KEY_ENV);
		for(final String concurrencyValue : concurrencyValues.split(",")) {
			includedConcurrencies.add(Concurrency.valueOf(concurrencyValue.toUpperCase()));
		}

		final List<ItemSize> includedItemSizes = new ArrayList<>();
		final String itemSizeValues = env.get(ItemSize.KEY_ENV);
		for(final String itemSizeValue : itemSizeValues.split(",")) {
			includedItemSizes.add(ItemSize.valueOf(itemSizeValue.toUpperCase()));
		}

		for(final StorageType storageType : includedStorageTypes) {
			for(final NodeCount nodeCount : includedNodeCounts) {
				for(final Concurrency concurrency : includedConcurrencies) {
					for(final ItemSize itemSize : includedItemSizes) {
						PARAMS.add(
							new Object[] {
								storageType, nodeCount, concurrency, itemSize
							}
						);
					}
				}
			}
		}
	}

	// prevent the instantiation
	private EnvParams() {
	}
}
