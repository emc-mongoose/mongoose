package com.emc.mongoose.common.io.value;

import com.emc.mongoose.common.io.Input;
import com.emc.mongoose.common.io.value.FilePathInput;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class FilePathInputRandomTest {

	private Input<String> formatter;

	private void initFormatter(int width, int depth) throws Exception {
		formatter = new FilePathInput(width, depth);
	}

	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{36, 4},
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
