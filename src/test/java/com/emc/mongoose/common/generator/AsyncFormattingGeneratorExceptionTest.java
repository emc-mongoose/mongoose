package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorExceptionTest extends AsyncFormattingGeneratorTestBase {

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%%d"},
				{"%d[1-5]%"},
				{"nghgh%"},
				{"%%%%%%"},
				{"% "},
				{"hgdhgdh%xgfht"},
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Test(expected = IllegalArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(patternString);
		formatter.get();
	}

}