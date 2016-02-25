package com.emc.mongoose.common.generator;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncRangeGeneratorFactory.*;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorBasicTest
extends TestCase {

	private final static Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE_REG_EXP);
	private final static Pattern LONG_PATTERN = Pattern.compile(LONG_REG_EXP);
	private final static Pattern DATE_PATTERN = Pattern.compile(DATE_REG_EXP);

	protected ValueGenerator<String> formatter;

	protected void initFormatter(String patternString) throws Exception {
		formatter = new AsyncFormattingGenerator(patternString);
		while (null == formatter.get()) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
	}

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%d", LONG_PATTERN},
				{"%d[1-5]", LONG_PATTERN},
				{"%f", DOUBLE_PATTERN},
				{"%f[0.1-5.0]", DOUBLE_PATTERN},
				{"%D", DATE_PATTERN},
				{"%D[1999/02/15-2014/08/22]", DATE_PATTERN},
				{"%D[2016/1/1-2016/1/31]", DATE_PATTERN},
				{"%D[1973/12/15-1973/12/16]", DATE_PATTERN},
				{"%D[2016/11/21-2016/12/21]", DATE_PATTERN},
				{"%D[2015/2/2-2015/3/30]", DATE_PATTERN},
				{"%D[2015/2/2-2015/3/3]", DATE_PATTERN},
				{"fdfdsfghfh %f[-987.0--785.5]gdghhfe", DOUBLE_PATTERN}
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Parameter(value = 1)
	public Pattern resultPattern;

	@Test
	public void checkFormattingResult() throws Exception {
		initFormatter(patternString);
		final String result = formatter.get();
		assertTrue(result, resultPattern.matcher(result).find());
	}

}
