package com.emc.mongoose.common.generator;

import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

public class SimpleFormattingGeneratorNullTest {

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new SimpleFormattingGenerator(patternString);
	}

	@Test(expected = NullArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(null);
		formatter.get();
	}
}