package com.emc.mongoose.base.item.naming;

import org.junit.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.akurilov.commons.math.MathUtil.xorShift;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ItemNamingTest {

	@Test
	public final void testRandomItemNames()
	throws Exception {
		try(
			final var in = ItemNameInput.Builder.newInstance()
				.length(13)
				.radix(36)
				.seed(1)
				.type(ItemNameInput.ItemNamingType.RANDOM)
				.build()
		) {
			assertEquals("l84y9", in.get());
			assertEquals("b4nnpm0nlt", in.get());
			assertEquals("1xxfsxb3qow5p", in.get());
			assertEquals("13f6twrd4lr3", in.get());
			assertEquals("j6b66dggi1", in.get());
			assertEquals("3ilqcpam2j6w", in.get());
			assertEquals("16mm0z6qsh3s8", in.get());
			assertEquals("28w3hpfrwuz", in.get());
			assertEquals("1e7tjj0vhz3rp", in.get());
			assertEquals("7j7u4icn21am", in.get());
		}
	}

	@Test
	public final void testAscItemNames()
	throws Exception {
		try(
			final var in = ItemNameInput.Builder.newInstance()
				.radix(10)
				.seed(0)
				.step(1)
				.type(ItemNameInput.ItemNamingType.SERIAL)
				.build()
		) {
			assertEquals("1", in.get());
			assertEquals("2", in.get());
			assertEquals("3", in.get());
			assertEquals("4", in.get());
			assertEquals("5", in.get());
			assertEquals("6", in.get());
			assertEquals("7", in.get());
			assertEquals("8", in.get());
			assertEquals("9", in.get());
			assertEquals("10", in.get());
		}
	}

	@Test
	public final void testDescItemNamesWithPrefix()
	throws Exception {
		try(
			final var in = ItemNameInput.Builder.newInstance()
				.prefix("prefix-")
				.radix(10)
				.seed(10)
				.step(-2)
				.type(ItemNameInput.ItemNamingType.SERIAL)
				.build()
		) {
			assertEquals("prefix-8", in.get());
			assertEquals("prefix-6", in.get());
			assertEquals("prefix-4", in.get());
			assertEquals("prefix-2", in.get());
			assertEquals("prefix-0", in.get());
			assertEquals("prefix-2", in.get());
			assertEquals("prefix-0", in.get());
			assertEquals("prefix-2", in.get());
			assertEquals("prefix-0", in.get());
			assertEquals("prefix-2", in.get());
		}
	}

	@Test
	public final void testItemNamesWithDynamicPrefix()
	throws Exception {
		final var p = Pattern.compile("\\d{1,20}_(\\d{1,2})");
		try(
			final var in = ItemNameInput.Builder.newInstance()
				.prefix("${time:nanos()}_")
				.radix(10)
				.seed(0)
				.step(1)
				.type(ItemNameInput.ItemNamingType.SERIAL)
				.build()
		) {
			String r;
			Matcher m;
			for(var i = 1; i <= 10; i ++) {
				r = in.get();
				m = p.matcher(r);
				assertTrue(m.find());
				assertEquals(i, Integer.parseInt(m.group(1)));
			}

		}
	}
}
