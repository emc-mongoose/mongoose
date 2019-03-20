package com.emc.mongoose.base.item.naming;

import com.emc.mongoose.base.config.ConstantValueInputImpl;
import com.emc.mongoose.base.config.el.CompositeExpressionInputBuilder;
import com.github.akurilov.commons.io.Input;

import static com.emc.mongoose.base.item.naming.ItemNameInput.ItemNamingType;
import static com.emc.mongoose.base.item.naming.ItemNameInput.ItemNamingType.RANDOM;
import static com.github.akurilov.commons.math.MathUtil.xorShift;
import static java.lang.Math.abs;
import static java.lang.Math.pow;

public final class ItemNameInputBuilder
				implements ItemNameInput.Builder {

	private volatile ItemNamingType type = RANDOM;
	private volatile int radix = Character.MAX_RADIX;
	private volatile String prefix = null;
	private volatile int length = 12;
	private volatile long seed = 0;
	private volatile int step = 1;

	@Override
	public final ItemNameInputBuilder type(final ItemNamingType type) {
		this.type = type;
		return this;
	}

	@Override
	public final ItemNameInputBuilder radix(final int radix) {
		this.radix = radix;
		return this;
	}

	@Override
	public final ItemNameInputBuilder prefix(final String prefix) {
		this.prefix = prefix;
		return this;
	}

	@Override
	public final ItemNameInputBuilder length(final int length) {
		this.length = length;
		return this;
	}

	@Override
	public final ItemNameInputBuilder seed(final long seed) {
		this.seed = seed;
		return this;
	}

	@Override
	public final ItemNameInputBuilder step(final int step) {
		this.step = step;
		return this;
	}

	@Override
	public <T extends ItemNameInput> T build() {
		final var maxId = (long) pow(radix, length);
		Input<String> prefixInput;
		if (prefix == null) {
			prefixInput = new ConstantValueInputImpl<>("");
		} else {
			prefixInput = CompositeExpressionInputBuilder.newInstance()
							.expression(prefix)
							.build();
		}
		switch (type) {
		case RANDOM:
			return (T) new ItemNameInputImpl((x) -> abs(xorShift(x) % maxId), seed, prefixInput, radix);
		case SERIAL:
			return (T) new ItemNameInputImpl((x) -> abs((x + step) % maxId), seed, prefixInput, radix);
		}
		return null;
	}
}
