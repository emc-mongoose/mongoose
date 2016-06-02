package com.emc.mongoose.common.conf;
import static org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
/**
 Created by kurila on 01.03.16.
 */
@RunWith(Parameterized.class)
public class SizeInBytesFormatTest {
	@Parameterized.Parameters
	public static Iterable<Object[]> data() {
		return Arrays.asList(
			new Object[][] {
				{"0B", new SizeInBytes(0, 0, 1)},
				{"1B", new SizeInBytes(1, 1, 1)},
				{"21KB", new SizeInBytes(21*1024, 21*1024, 1)},
				{"321MB", new SizeInBytes(321*1048576, 321*1048576, 1)},
				{"4.460GB", new SizeInBytes(4567L*1048576, 4567L*1048576, 1)},
				{"890TB", new SizeInBytes(890L*1048576*1048576, 890L*1048576*1048576, 1)},
				{"0B-1B", new SizeInBytes(0, 1, 1)},
				{"21KB-321MB", new SizeInBytes(21*1024, 321*1048576, 1)},
				{"4.460GB-890TB,0.5", new SizeInBytes(4567L*1048576, 890L*1048576*1048576, 0.5)},
				{"4.460GB-890TB,1.5", new SizeInBytes(4567L*1048576, 890L*1048576*1048576, 1.5)}
			}
		);
	}

	@Parameterized.Parameter(value = 0)
	public String sizeInfoString;

	@Parameterized.Parameter(value = 1)
	public SizeInBytes sizeInBytes;

	@Test
	public void checkParsing()
	throws Exception {
		assertEquals(sizeInfoString, sizeInBytes.toString());
	}
}
