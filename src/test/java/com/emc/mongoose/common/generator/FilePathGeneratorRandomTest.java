package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.generator.FilePathGenerator.DIR_NAME_PREFIX;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FilePathGeneratorRandomTest {

	private static final String PATH_REG_EXP = "(" + DIR_NAME_PREFIX + "[0-9a-z]+" + "\\/" + ")+";
	private static final Pattern PATH_PATTERN = Pattern.compile(PATH_REG_EXP);

	private ValueGenerator<String> formatter;

	private void initFormatter(int width, int depth) throws Exception {
		formatter = new FilePathGenerator(width, depth);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{5, 2},
		});
	}

	@Parameterized.Parameter(value = 0)
	public int width;

	@Parameterized.Parameter(value = 1)
	public int depth;

	@Test
	public void checkPrintingResult() throws Exception {
		initFormatter(width, depth);
		String result1 = formatter.get();
		for (int i = 0; i < 10; i++) {
			String result2 = formatter.get();
			assertFalse(result1.equals(result2));
			result1 = result2;
		}
	}


}