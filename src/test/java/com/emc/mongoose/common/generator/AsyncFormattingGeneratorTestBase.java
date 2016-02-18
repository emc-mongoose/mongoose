package com.emc.mongoose.common.generator;

import junit.framework.TestCase;

import java.util.concurrent.locks.LockSupport;

public class AsyncFormattingGeneratorTestBase extends TestCase {

	protected ValueGenerator<String> formatter;

	protected void initFormatter(String patternString) throws Exception {
		formatter = new AsyncFormattingGenerator(patternString);
		while (null == formatter.get()) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
//		Thread.sleep(100); // todo Thread.yield is not enough for an unknown reason
	}

}
