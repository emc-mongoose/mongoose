package com.emc.mongoose.base.config.el;

import static com.emc.mongoose.base.config.el.ExpressionInputBuilder.EXPRESSION_PATTERN;
import static com.emc.mongoose.base.config.el.ExpressionInputBuilderImpl.INITIAL_VALUE_PATTERN;
import static com.emc.mongoose.base.env.DateUtil.FMT_DATE_METRICS_TABLE;
import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.emc.mongoose.base.env.DateUtil;
import com.github.akurilov.commons.io.el.ExpressionInput;
import com.github.akurilov.commons.io.el.SynchronousExpressionInput;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class ExpressionInputTest {

	@Test
	public void testBuiltinCall() throws Exception {
		final var in = (AsyncExpressionInput<Long>) ExpressionInputBuilder.newInstance()
						.expression("#{time:millisSinceEpoch()}")
						.type(long.class)
						.<Long, ExpressionInput<Long>> build();
		assertNull(in.get());
		in.start();
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue(100 >= currentTimeMillis() - in.get());
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue(100 >= currentTimeMillis() - in.get());
		TimeUnit.MILLISECONDS.sleep(100);
		assertTrue(100 >= currentTimeMillis() - in.get());
		in.stop();
		final var last = in.get();
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(last, in.get());
		TimeUnit.MILLISECONDS.sleep(100);
		assertEquals(last, in.get());
		in.close();
		assertNull(in.get());
	}

	@Test
	public void testRandomItemId() throws Exception {
		final var radix = 36;
		final var length = 10;
		final var init = "%{math:absInt64(int64:xor(int64:reverse(time:millisSinceEpoch()), int64:reverseBytes(time:nanos())))}";
		final var expr = "${math:absInt64(int64:xorShift(this.last()) % math:pow(radix, length))}";
		final var offsetInput = ExpressionInputBuilder.newInstance()
						.expression(init + expr)
						.type(long.class)
						.value("radix", radix, int.class)
						.value("length", length, int.class)
						.<Long, SynchronousExpressionInput<Long>> build();
		final var itemNameInput = ExpressionInputBuilder.newInstance()
						.expression("${int64:toString(offsetInput.get(), radix)}")
						.type(String.class)
						.value("offsetInput", offsetInput, SynchronousExpressionInput.class)
						.value("radix", radix, int.class)
						.<String, SynchronousExpressionInput<String>> build();
		final var id = itemNameInput.get();
		assertTrue(length >= id.length());
		final var offset = offsetInput.last();
		assertTrue(Long.toString(offset, radix).endsWith(id));
	}

	@Test
	public void testVararg() throws Exception {
		final var inputBuilder = ExpressionInputBuilder.newInstance().type(String.class);
		var in = inputBuilder
						.expression("${string:join('_', 'a')}")
						.<String, ExpressionInput<String>> build();
		assertEquals("a", in.get());
		in.close();
		in = inputBuilder.expression("${string:join('_', 'a', 'b')}").build();
		assertEquals("a_b", in.get());
		in.close();
	}

	@Test
	public void testPaths() throws Exception {
		final var exprPathInput = ExpressionInputBuilder.newInstance()
						.type(String.class)
						.expression("/${path:random(16, 2)}")
						.<String, ExpressionInput<String>> build();
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
		final var withInitVal = "prefix_%{-1}${this.last() + 1}suffix";
		var m = INITIAL_VALUE_PATTERN.matcher(withInitVal);
		assertTrue(m.find());
		assertEquals("%{-1}", m.group(1));
		final var noInitVal = "prefix_${this.last() + 1}suffix";
		m = INITIAL_VALUE_PATTERN.matcher(noInitVal);
		assertFalse(m.find());
	}

	@Test
	public void testDateFormatRfc1123() throws Exception {
		final var d0 = DateUtil.FMT_DATE_RFC1123.format(new Date(System.currentTimeMillis()));
		try (final var dateInput = ExpressionInputBuilder.newInstance()
						.type(String.class)
						.expression("#{date:formatNowRfc1123()}")
						.initial(d0)
						.<String, AsyncExpressionInput<String>> build()) {
			final var d1 = dateInput.get();
			assertEquals(d0, d1);
			dateInput.start();
			TimeUnit.MILLISECONDS.sleep(2000);
			final var d2 = dateInput.get();
			final var date2 = DateUtil.FMT_DATE_RFC1123.parse(d2);
			final var date1 = DateUtil.FMT_DATE_RFC1123.parse(d1);
			assertTrue(date2.after(date1));
		}
	}

	@Test
	public void testRandomDateInRangeCustomFormat()
					throws Exception {
		final var dateInput = ExpressionInputBuilder.newInstance()
						.type(String.class)
						.expression("${date:format(\"" + DateUtil.PATTERN_METRICS_TABLE + "\").format(date:from(rnd.nextLong(time:millisSinceEpoch())))}")
						.<String, ExpressionInput<String>> build();
		final var rndDateStr = dateInput.get();
		final var rndDate = FMT_DATE_METRICS_TABLE.parse(rndDateStr);
		assertTrue(rndDate.after(new Date(0)));
		assertTrue(rndDate.before(new Date(currentTimeMillis())));
	}

	@Test
	public void testMessageFormat()
					throws Exception {
		final var msgInput = ExpressionInputBuilder.newInstance()
						.type(String.class)
						.expression("${string:format(\"At {1,time} on {1,date}, there was {2} on planet {0,number,integer}.\", 7, date:from(0), \"a disturbance in the Force\")}")
						.build();
		assertEquals("At 00:00:00 on 1970 Jan 1, there was a disturbance in the Force on planet 7.", msgInput.get());
	}

	@Test
	public void testConstantValueExpression()
					throws Exception {
		final var t0 = System.currentTimeMillis();
		final var in = ExpressionInputBuilder.newInstance()
						.expression("%{time:millisSinceEpoch()}")
						.type(long.class)
						.<Long, ExpressionInput<Long>> build();
		final var t1 = in.get();
		final var t2 = System.currentTimeMillis();
		assertTrue(t0 < t1);
		assertTrue(t1 < t2);
		TimeUnit.SECONDS.sleep(5); // wait, maybe the value will change...
		final var t3 = in.get();
		assertEquals(t1, t3); // no, it has not been changed
	}

	@Test
	public void testExpressionWithoutTypeSpecified()
					throws Exception {
		final var in = ExpressionInputBuilder.newInstance()
						.expression("%{time:millisSinceEpoch()}")
						.type(Object.class)
						.build();
		final var x = in.get();
		System.out.println(x);
	}

	@Test
	public void testCompositeExpression()
					throws Exception {
		final var e = "prefix%{time:millisSinceEpoch()}foo${rnd.nextInt(42)}bar${this.last() + 1}%{-1}____#{date:formatNowRfc1123()}#{this.last() + 1}%{0}suffix";
		final var m = EXPRESSION_PATTERN.matcher(e);
		var start = 0;
		var end = 0;
		final var strb = new StringBuilder();
		while (m.find()) {
			end = m.start();
			if (end > 0) {
				strb.append(e, start, end);
			}
			start = m.end();
			final var expr = m.group("expr");
			final var init = m.group("init");
			if (expr != null || init != null) {
				System.out.println('"' + strb.toString() + '"');
				strb.setLength(0);
				System.out.println("expr: \"" + m.group("expr") + "\", init: \"" + m.group("init") + '"');
			}
		}
		System.out.println('"' + strb.toString() + '"');
		strb.setLength(0);
	}
}
