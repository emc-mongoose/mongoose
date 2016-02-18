package com.emc.mongoose.common.generator;

import junit.framework.TestCase;
import org.junit.BeforeClass;
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
public class AsyncFormattingGeneratorNullTest
extends TestCase {

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
				{"%d"},
				{"%d[1-5]"},
				{"%f"},
				{"%f[0.1-5.0]"},
//				{"%D"},
//				{"%D[1999/02/15-2014/08/22]"},
				{"fdfdsfghfh %f[-987.0--785.5]gdghhfe"}
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Test
	public void checkFormattingReturn() throws Exception {
		initFormatter(patternString);
		assertNotNull(formatter.get());
	}

}
