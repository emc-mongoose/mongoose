package com.emc.mongoose.common.generator;

import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;
public class BasicFormattingGeneratorNullTest {

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new BasicFormattingGenerator(patternString);
	}

	@Test(expected = NullArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(null);
		formatter.get();
	}
}
