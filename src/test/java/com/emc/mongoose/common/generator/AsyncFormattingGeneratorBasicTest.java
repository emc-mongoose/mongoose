package com.emc.mongoose.common.generator;

import junit.framework.TestCase;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.AsyncRangeGeneratorFactory.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorBasicTest extends AsyncFormattingGeneratorTestBase {

	public static Pattern doublePattern = Pattern.compile(doubleRegExp);
	public static Pattern longPattern = Pattern.compile(longRegExp);
	public static Pattern datePattern = Pattern.compile(dateRegExp);

	@BeforeClass
	public static void initPatterns() {
		doublePattern = Pattern.compile(doubleRegExp);
		longPattern = Pattern.compile(longRegExp);
		datePattern = Pattern.compile(dateRegExp);
	}

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%d", longPattern},
				{"%d[1-5]", longPattern},
				{"%f", doublePattern},
				{"%f[0.1-5.0]", doublePattern},
				{"%D", datePattern},
				{"%D[1999/02/15-2014/08/22]", datePattern},
				{"fdfdsfghfh %f[-987.0--785.5]gdghhfe", doublePattern}
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Parameter(value = 1)
	public Pattern resultPattern;

	@Test
	public void checkFormatting() throws Exception {
		initFormatter(patternString);
		String result = formatter.get();
		assertThat(resultPattern.matcher(result).find(), equalTo(true));
	}

}