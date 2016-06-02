package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.FilePathInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static com.emc.mongoose.common.io.value.StringInputFactory.PATH_REG_EXP;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class FilePathInputBasicTest {

	private static final Pattern PATH_PATTERN = Pattern.compile(PATH_REG_EXP);

	private Input<String> formatter;

	private void initFormatter(int width, int depth) throws Exception {
		formatter = new FilePathInput(width, depth);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{5, 2},
				{2, 3},
				{35, 6},
				{7, 11},
				{1, 1},
		});
	}

	@Parameterized.Parameter(value = 0)
	public int width;

	@Parameterized.Parameter(value = 1)
	public int depth;

	@Test
	public void checkPrintingResult() throws Exception {
		initFormatter(width, depth);
		final String result = formatter.get();
//		System.out.println(result);
		assertTrue(PATH_PATTERN.matcher(result).find());
	}


}
