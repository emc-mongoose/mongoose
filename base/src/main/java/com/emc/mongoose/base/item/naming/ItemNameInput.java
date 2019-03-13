package com.emc.mongoose.base.item.naming;

import com.github.akurilov.commons.io.Input;

import java.util.List;

public interface ItemNameInput
				extends Input<String> {

	long lastId();

	enum ItemNamingType {
		RANDOM, SERIAL
	}

	@Override
	String get();

	@Override
	int get(final List<String> buffer, final int limit);

	interface Builder {

		<T extends Builder> T type(final ItemNamingType type);

		<T extends Builder> T radix(final int radix);

		<T extends Builder> T prefix(final String prefix);

		<T extends Builder> T length(final int length);

		<T extends Builder> T seed(final long offset);

		<T extends Builder> T step(final int step);

		<T extends ItemNameInput> T build();

		static <T extends Builder> T newInstance() {
			return (T) new ItemNameInputBuilder();
		}
	}
}
