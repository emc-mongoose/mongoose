package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.circularity.CircularReadAfterUpdateTest;
import com.emc.mongoose.system.feature.circularity.CircularReadFromBucketTest;
import com.emc.mongoose.system.feature.circularity.CircularReadTest;
import com.emc.mongoose.system.feature.circularity.CircularUpdateTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	CircularReadAfterUpdateTest.class,
	CircularReadFromBucketTest.class,
	CircularReadTest.class,
	CircularUpdateTest.class
})
public class CircularLoadTestSuite {
}
