package com.emc.mongoose.common.generator;

import com.emc.mongoose.common.generator.async.AsyncFormattingGenerator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.concurrent.locks.LockSupport;

import static org.junit.runners.Parameterized.Parameter;
import static org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class AsyncFormattingGeneratorExceptionTest {

	protected ValueGenerator<String> formatter;

	protected void initFormatter(String patternString) throws Exception {
		formatter = new AsyncFormattingGenerator(patternString);
		while (null == formatter.get()) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
	}

	@Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(new Object[][]{
				{"%%d"},
				{"%d[1-5]%"},
				{"nghgh%"},
				{"%%%%%%"},
				{"% "},
				{"%D{gfg}"},
				{"%f{fdl;}"},
		});
	}
	@Parameter(value = 0)
	public String patternString;

	@Test(expected = IllegalArgumentException.class)
	public void checkExceptionThrowing() throws Exception {
		initFormatter(patternString);
		formatter.get();
	}

}
