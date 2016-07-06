package com.emc.mongoose.unit;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.BasicPatternDefinedInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

@RunWith(Parameterized.class)
public class BasicPatternDefinedInputExceptionTest {

	private Input<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new BasicPatternDefinedInput(patternString);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"dfdf$p{1; 3}"},
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
