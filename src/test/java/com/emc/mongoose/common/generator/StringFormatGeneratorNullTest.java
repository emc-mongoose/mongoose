package com.emc.mongoose.common.generator;

import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;
public class StringFormatGeneratorNullTest {

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new StringFormatGenerator(patternString);
	}

	@Test(expected = NullArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(null);
		formatter.get();
	}
}
