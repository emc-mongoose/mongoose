package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.content.ReadRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.content.ReadZeroBytesTest;
import com.emc.mongoose.integ.feature.content.UpdateRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.content.UpdateZeroBytesTest;
import com.emc.mongoose.integ.feature.content.WriteRikkiTikkiTaviTest;
import com.emc.mongoose.integ.feature.content.WriteZeroBytesTest;
import com.emc.mongoose.integ.feature.content.UpdateZeroBytesDistributedTest;
import com.emc.mongoose.integ.feature.content.WriteRikkiTikkiTaviDistributedTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteZeroBytesTest.class,
	ReadZeroBytesTest.class,
	UpdateZeroBytesTest.class,
	WriteRikkiTikkiTaviTest.class,
	ReadRikkiTikkiTaviTest.class,
	UpdateRikkiTikkiTaviTest.class,
	//
	UpdateZeroBytesDistributedTest.class,
	WriteRikkiTikkiTaviDistributedTest.class,
})
public class ContentSuite {
}
