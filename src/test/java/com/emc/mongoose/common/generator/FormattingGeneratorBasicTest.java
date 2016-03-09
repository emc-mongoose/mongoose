package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.FilePathGenerator.DIR_NAME_PREFIX;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class FormattingGeneratorBasicTest {

	private static final String PATH_REG_EXP = "(" + DIR_NAME_PREFIX + "[0-9a-z]+" + "\\/" + ")+";
	private static final Pattern PATH_PATTERN = Pattern.compile(PATH_REG_EXP);

	private ValueGenerator<String> formatter;

	private void initFormatter(String patternString) throws Exception {
		formatter = new SimpleFormattingGenerator(patternString);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%p{1; 3}", PATH_PATTERN },
				{"%p{11; 7}", PATH_PATTERN },
				{"%p{1; 1}", PATH_PATTERN },
		});
	}

	@Parameterized.Parameter(value = 0)
	public String patternString;

	@Parameterized.Parameter(value = 1)
	public Pattern resultPattern;

	@Test
	public void checkFormattingResult() throws Exception {
		initFormatter(patternString);
		final String result = formatter.get();
		assertTrue(result, resultPattern.matcher(result).find());
		assertEquals(numberOfSpaces(patternString), numberOfSpaces(result));
		System.out.println(result);
	}

	private int numberOfSpaces(String string) {
		int counter = 0;
		for (char each: string.toCharArray()) {
			if (each == ' ') {
				counter++;
			}
			if (each == ';') { // a temp condition
				counter--;
			}
		}
		return counter;
	}

}