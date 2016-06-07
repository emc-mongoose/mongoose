package com.emc.mongoose.unit;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.async.AsyncPatternDefinedInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

import static org.junit.Assert.*;
import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncPatternDefinedInputNullTest {

	private static final String OUTPUT_NUMBER_FMT_STRING = "$f" + "{" + "###.##" + "}";
	private static final String OUTPUT_DATE_FMT_STRING = "$D" + "{" + "yyyy-MM-dd'T'HH:mm:ssZ" + "}";

	protected Input<String> formatter;

	protected void initFormatter(String patternString) throws Exception {
		formatter = new AsyncPatternDefinedInput(patternString);
		while (null == formatter.get()) {
			Thread.yield();
		}
	}

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"$d"},
				{"$d[1-5]"},
				{OUTPUT_NUMBER_FMT_STRING},
				{OUTPUT_NUMBER_FMT_STRING + "[0.1-5.0]"},
				{OUTPUT_DATE_FMT_STRING},
				{OUTPUT_DATE_FMT_STRING + "[1999/02/15-2014/08/22]"},
				{"fdfdsfghfh " + OUTPUT_NUMBER_FMT_STRING + "[-987.0--785.5]gdghhfe"}
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
