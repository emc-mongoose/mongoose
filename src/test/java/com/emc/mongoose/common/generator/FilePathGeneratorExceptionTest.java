package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;


@RunWith(Parameterized.class)
public class FilePathGeneratorExceptionTest {

	private ValueGenerator<String> formatter;

	private void initFormatter(int width, int depth) throws Exception {
		formatter = new FilePathGenerator(width, depth);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{0, 0},
				{0, 5},
				{35, 0},
				{-7, -11},
		});
	}

	@Parameterized.Parameter(value = 0)
	public int width;

	@Parameterized.Parameter(value = 1)
	public int depth;

	@Test(expected = IllegalArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(width, depth);
		formatter.get();
	}

}
