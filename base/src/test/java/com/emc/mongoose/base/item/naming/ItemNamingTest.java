package com.emc.mongoose.base.item.naming;

import org.junit.Test;

import static com.github.akurilov.commons.math.MathUtil.xorShift;
import static org.junit.Assert.assertEquals;

public class ItemNamingTest {

	@Test
	public final void testRandomItemNames()
	throws Exception {
		final var length = 13;
		final var radix = Character.MAX_RADIX;
		final var max = (long) Math.pow(radix, length);
		try(
			final var in = ItemNameInput.Builder.newInstance()
				.length(length)
				.radix(radix)
				.type(ItemNameInput.ItemNamingType.RANDOM)
				.build()
		) {
			String next;
			long prevNum = 0;
			long nextNum;
			for(var i = 0; i < 1_000; i ++) {
				next = in.get();
				nextNum = Long.parseLong(next, Character.MAX_RADIX);
				if(0 != prevNum) {
					assertEquals(xorShift(prevNum) % max, nextNum);
				}
				prevNum = nextNum;
			}
		}
	}

}
