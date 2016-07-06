package com.emc.mongoose.system.suite;

import com.emc.mongoose.system.feature.content.ReadRikkiTikkiTaviTest;
import com.emc.mongoose.system.feature.content.ReadZeroBytesTest;
import com.emc.mongoose.system.feature.content.UpdateRikkiTikkiTaviTest;
import com.emc.mongoose.system.feature.content.UpdateZeroBytesTest;
import com.emc.mongoose.system.feature.content.WriteRikkiTikkiTaviTest;
import com.emc.mongoose.system.feature.content.WriteZeroBytesTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 Created by kurila on 23.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ReadRikkiTikkiTaviTest.class,
	ReadZeroBytesTest.class,
	UpdateRikkiTikkiTaviTest.class,
	UpdateZeroBytesTest.class,
	WriteRikkiTikkiTaviTest.class,
	WriteZeroBytesTest.class,
})
public class ContentTestSuite {
}
