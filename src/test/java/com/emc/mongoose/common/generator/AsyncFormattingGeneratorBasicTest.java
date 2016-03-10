package com.emc.mongoose.common.generator;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncStringGeneratorFactory.DOUBLE_REG_EXP;
import static com.emc.mongoose.common.generator.AsyncStringGeneratorFactory.LONG_REG_EXP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorBasicTest {

	private static final String OUTPUT_NUMBER_FMT_STRING = "%f" + "{" + "###.##" + "}";
	private static final String OUTPUT_DATE_FMT_STRING = "%D" + "{" + "yyyy-MM-dd'T'HH:mm:ssZ" + "}";
	private static final String DATE_REG_EXP =
			"(((19|20)[0-9][0-9])-(1[012]|0?[1-9])-(3[01]|[12][0-9]|0?[1-9])T(0[0-9]|1[0-9]|2[0-4]):([0-5][0-9]):([0-5][0-9]))"; // regexp should match the date format in the string above
	private static final Pattern ANYTHING_PATTERN = Pattern.compile(".*");
	private static final Pattern DOUBLE_PATTERN = Pattern.compile(DOUBLE_REG_EXP);
	private static final Pattern LONG_PATTERN = Pattern.compile(LONG_REG_EXP);
	private static final Pattern DATE_PATTERN = Pattern.compile(DATE_REG_EXP);

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new AsyncFormattingGenerator(patternString);
		while (null == formatter.get()) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
	}

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"dfdgffsgsgsg", ANYTHING_PATTERN},
				{"%d", LONG_PATTERN},
				{"%d[1-5]", LONG_PATTERN},
				{OUTPUT_NUMBER_FMT_STRING, DOUBLE_PATTERN},
				{OUTPUT_NUMBER_FMT_STRING + "[0.1-5.0]", DOUBLE_PATTERN},
				{OUTPUT_DATE_FMT_STRING, DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[1999/02/15-2014/08/22]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2016/1/1-2016/1/31]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2015/1/1-2015/1/2]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2016/1/1-2016/1/2]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[1973/12/15-1973/12/16]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2016/11/21-2016/12/21]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2015/2/2-2015/3/30]", DATE_PATTERN},
				{OUTPUT_DATE_FMT_STRING + "[2015/2/2-2015/3/3]", DATE_PATTERN},
				{"fdfdsfghfh" + OUTPUT_NUMBER_FMT_STRING + "[-987.0--785.5]gdghhfe", DOUBLE_PATTERN}
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Parameter(value = 1)
	public Pattern resultPattern;

	@Test
	public void checkFormattingResult() throws Exception {
		initFormatter(patternString);
//		Thread.sleep(3000); // to give an opportunity for the generator to work
		final String result = formatter.get();
//		System.out.println(patternString + ": " + formatter.get()); // to check that the result is within range
		assertTrue(result, resultPattern.matcher(result).find());
		assertEquals(numberOfSpaces(patternString), numberOfSpaces(result));
	}

	private int numberOfSpaces(String string) {
		int counter = 0;
		for (char each: string.toCharArray()) {
			if (each == ' ') {
				counter++;
			}
		}
		return counter;
	}

}
