package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncRangeGeneratorFactory.*;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorBasicTest extends AsyncFormattingGeneratorTestBase {

	private final static Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE_REG_EXP);
	private final static Pattern LONG_PATTERN = Pattern.compile(LONG_REG_EXP);
	private final static Pattern DATE_PATTERN = Pattern.compile(DATE_REG_EXP);

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%d", LONG_PATTERN},
				{"%d[1-5]", LONG_PATTERN},
				{"%f", DOUBLE_PATTERN},
				{"%f[0.1-5.0]", DOUBLE_PATTERN},
				{"%D", DATE_PATTERN},
				{"%D[1999/02/15-2014/08/22]", DATE_PATTERN},
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
