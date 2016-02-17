package com.emc.mongoose.common.generator;

import junit.framework.TestCase;

import java.util.concurrent.locks.LockSupport;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class AsyncFormattingGeneratorTest extends TestCase {

	public void testFormat() throws Exception {
		final ValueGenerator<String>
				formatter = new AsyncFormattingGenerator("");
		while(null == formatter.get()) {
			LockSupport.parkNanos(1);
			Thread.yield();
		}
		assertThat(formatter.get(), is(""));
	}

}