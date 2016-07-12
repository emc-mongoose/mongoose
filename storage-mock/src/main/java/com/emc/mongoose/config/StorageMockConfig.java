package com.emc.mongoose.config;

/**
 Created on 11.07.16.
 */
public class StorageMockConfig {

	public static final String KEY_HEAD_COUNT = "headCount";
	public static final String KEY_CAPACITY = "capacity";
	public static final String KEY_CONTAINER = "container";
	private final int headCount;
	private final int capacity;
	private final Container container;

	public StorageMockConfig(
		final int headCount, final int capacity, final Container container
	) {
		this.headCount = headCount;
		this.capacity = capacity;
		this.container = container;
	}

	public int getHeadCount() {
		return headCount;
	}

	public int getCapacity() {
		return capacity;
	}

	public Container container() {
		return container;
	}

	public static class Container {

		public static final String KEY_CAPACITY = "capacity";
		public static final String KEY_COUNT_LIMIT = "countLimit";
		private final int capacity;
		private final int countLimit;

		public Container(final int capacity, final int countLimit) {
			this.capacity = capacity;
			this.countLimit = countLimit;
		}

		public int getCapacity() {
			return capacity;
		}

		public int getCountLimit() {
			return countLimit;
		}
	}

}
