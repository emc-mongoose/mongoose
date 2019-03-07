package com.emc.mongoose.base.config.el;

import static com.emc.mongoose.base.config.el.CompositeExpressionInputBuilderImpl.INITIAL_VALUE_PATTERN;
import static com.emc.mongoose.base.env.DateUtil.FMT_DATE_METRICS_TABLE;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.emc.mongoose.base.env.DateUtil;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CompositeExpressionInputTest {

	@Test
	public void testRandomItemId() throws Exception {
		final var radix = 36;
		final var length = 10;
		final var init = "%{math:absInt64(int64:xor(int64:reverse(time:millisSinceEpoch()), int64:reverseBytes(time:nanos())))}";
		final var expr = "${math:absInt64(int64:xorShift(this.last()) % math:pow(radix, length))}";
		final var offsetInput = CompositeExpressionInputBuilder.newInstance()
						.expression(expr + init)
						.value("radix", radix, int.class)
						.value("length", length, int.class)
						.build();
		final var itemNameInput = CompositeExpressionInputBuilder.newInstance()
						.expression("${int64:toString(offsetInput.get(), radix)}")
						.value("offsetInput", offsetInput, SynchronousExpressionInput.class)
						.value("radix", radix, int.class)
						.build();
		final var id = itemNameInput.get();
		assertTrue(length >= id.length());
	}

	@Test
	public void testSelfReferenceInCompositeExpression()
					throws Exception {
		final var data = "Foo${this.expr()}Bar#{this.expr()}";
		final var in = CompositeExpressionInputBuilder.newInstance()
						.expression(data)
						.build();
		TimeUnit.MILLISECONDS.sleep(100);
		final var result = in.get();
		assertEquals(data, result);
	}

	@Test
	public void testVararg() throws Exception {
		final var inputBuilder = CompositeExpressionInputBuilder.newInstance();
		var in = inputBuilder
						.expression("${string:join('_', 'a')}")
						.build();
		assertEquals("a", in.get());
		in.close();
		in = inputBuilder.expression("${string:join('_', 'a', 'b')}").build();
		assertEquals("a_b", in.get());
		in.close();
	}

	@Test
	public void testPaths() throws Exception {
		final var exprPathInput = CompositeExpressionInputBuilder.newInstance()
						.expression("/${path:random(16, 2)}")
						.build();
		String p;
		String[] pp;
		for (var i = 0; i < 100; i++) {
			p = exprPathInput.get();
			assertTrue(p.startsWith("/"));
			assertTrue(p.endsWith("/"));
			pp = p.split("/", 4);
			assertTrue(pp.length > 2 && pp.length < 5);
			for (var ppp : pp) {
				if (!ppp.isEmpty()) {
					assertTrue(16 > Integer.parseInt(ppp, 16));
				}
			}
		}
	}

	@Test
	public void testInitialPattern() throws Exception {
		final var withInitVal = "prefix_${this.last() + 1}%{-1}suffix";
		var m = INITIAL_VALUE_PATTERN.matcher(withInitVal);
		assertTrue(m.find());
		assertEquals("%{-1}", m.group(1));
		final var noInitVal = "prefix_${this.last() + 1}suffix";
		m = INITIAL_VALUE_PATTERN.matcher(noInitVal);
		assertFalse(m.find());
	}

	@Test
	public void testRandomDateInRangeCustomFormat()
					throws Exception {
		final var dateInput = CompositeExpressionInputBuilder.newInstance()
						.expression("${date:format(\"" + DateUtil.PATTERN_METRICS_TABLE + "\").format(date:from(rnd.nextLong(time:millisSinceEpoch())))}")
						.build();
		final var rndDateStr = dateInput.get();
		final var rndDate = FMT_DATE_METRICS_TABLE.parse(rndDateStr);
		assertTrue(rndDate.after(new Date(0)));
		assertTrue(rndDate.before(new Date(currentTimeMillis())));
	}

	@Test
	public void testMessageFormat()
					throws Exception {
		final var msgInput = CompositeExpressionInputBuilder.newInstance()
						.expression("${string:format(\"At %tT %1$tZ on %1$tY %1$tb %1$te, there was %s on planet %d.\", date:from(0), \"a disturbance in the Force\", 7)}")
						.build();
		assertEquals("At 00:00:00 UTC on 1970 Jan 1, there was a disturbance in the Force on planet 7.", msgInput.get());
	}

	@Test
	public void testConstantValueExpression()
					throws Exception {
		final var t0 = System.currentTimeMillis();
		TimeUnit.SECONDS.sleep(1);
		final var in = CompositeExpressionInputBuilder.newInstance()
						.expression("%{time:millisSinceEpoch()}")
						.build();
		final var t1 = Long.parseLong(in.get());
		TimeUnit.SECONDS.sleep(1);
		final var t2 = System.currentTimeMillis();
		assertTrue(t0 < t1);
		assertTrue(t1 < t2);
		TimeUnit.SECONDS.sleep(1); // wait, maybe the value will change...
		final var t3 = Long.parseLong(in.get());
		assertEquals(t1, t3); // no, it has not been changed
		in.close();
	}
}
