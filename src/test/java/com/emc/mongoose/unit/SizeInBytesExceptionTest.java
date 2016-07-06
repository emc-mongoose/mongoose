package com.emc.mongoose.unit;
import com.emc.mongoose.common.conf.SizeInBytes;
import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
/**
 Created by kurila on 01.03.16.
 */
@RunWith(Parameterized.class)
public class SizeInBytesExceptionTest
extends TestCase {
	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(
			new Object[][] {
				{"0-"},
				{"-1B"},
				{"1WB"},
				{"1-0"},
				{"0-1,"},
				{"0,-1"}
			}
		);
	}

	@Parameterized.Parameter(value = 0)
	public String sizeInfoString;

	@Test(expected = IllegalArgumentException.class)
	public void checkException()
	throws Exception {
		new SizeInBytes(sizeInfoString);
	}
}
