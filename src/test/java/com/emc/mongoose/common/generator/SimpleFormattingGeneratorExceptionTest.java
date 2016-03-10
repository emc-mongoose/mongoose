package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class SimpleFormattingGeneratorExceptionTest {

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new SimpleFormattingGenerator(patternString);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"dfdf%p{1; 3}"},
				{"gdfhshg"},
		});
	}

	@Parameterized.Parameter(value = 0)
	public String patternString;

	@Test(expected = IllegalArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(patternString);
		formatter.get();
	}
}