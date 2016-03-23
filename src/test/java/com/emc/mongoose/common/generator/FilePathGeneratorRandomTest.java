package com.emc.mongoose.common.generator;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FilePathGeneratorRandomTest {

	private ValueGenerator<String> formatter;
	private static int[] counters;
	private static int depthToCount;

	private void initFormatter(int width, int depth) throws Exception {
		formatter = new FilePathGenerator(width, depth);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{5, 4},
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
		counters = new int[depth];
		depthToCount = depth;
		for (int i = 0; i < 100; i++) {
			String result2 = formatter.get();
//			System.out.println(formatter.get());
//			assertFalse(result1.equals(result2));
			countDirsByDepth(result1);
			printIfEquals(result1, result2);
			result1 = result2;
		}
		printCounters();
	}

	private void countDirsByDepth (final String result) {
		for (int i = 0; i < depthToCount; i++) {
			if (result.length() == ((i + 1) * 2)) {
				counters[i]++;
			}
		}
	}

	private void printCounters() {
		for (int counter: counters) {
			System.out.println(counter);
		}
	}

	private void printIfEquals(final String result1, final String result2) {
		if (result1.equals(result2)) {
				System.out.println("EQUALS!");
			};
	}
}
