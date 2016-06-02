package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.BasicPatternDefinedInput;
import org.apache.commons.lang.NullArgumentException;
import org.junit.Test;
public class BasicPatternDefinedInputNullTest {

	private Input<String> formatter;

	private void initPattern(String patternString) throws Exception {
		formatter = new BasicPatternDefinedInput(patternString);
	}

	@Test(expected = NullArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initPattern(null);
		formatter.get();
	}
}
