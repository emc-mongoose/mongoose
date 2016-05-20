package com.emc.mongoose.system.suite;
import com.emc.mongoose.system.feature.core.DefaultWriteTest;
import com.emc.mongoose.system.feature.core.InfiniteWriteTest;
import com.emc.mongoose.system.feature.core.Read10BItemsTest;
import com.emc.mongoose.system.feature.core.Read10KBItemsTest;
import com.emc.mongoose.system.feature.core.Read10MBItemsTest;
import com.emc.mongoose.system.feature.core.Read200MBItemsTest;
import com.emc.mongoose.system.feature.core.ReadVerificationTest;
import com.emc.mongoose.system.feature.core.ReadZeroSizeItemsTest;
import com.emc.mongoose.system.feature.core.WriteByCountTest;
import com.emc.mongoose.system.feature.core.WriteByTimeTest;
import com.emc.mongoose.system.feature.core.WriteRandomSizedItemsTest;
import com.emc.mongoose.system.feature.core.WriteUsing100ConnTest;
import com.emc.mongoose.system.feature.core.WriteUsing10ConnTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
/**
 Created by andrey on 20.05.16.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DefaultWriteTest.class,
	InfiniteWriteTest.class,
	WriteByCountTest.class,
	WriteByTimeTest.class,
	WriteRandomSizedItemsTest.class,
	WriteUsing10ConnTest.class,
	WriteUsing100ConnTest.class,
	Read10BItemsTest.class,
	Read10KBItemsTest.class,
	Read10MBItemsTest.class,
	Read200MBItemsTest.class,
	ReadVerificationTest.class,
	ReadZeroSizeItemsTest.class
})
public class CoreTestSuite {
}
