package com.emc.mongoose.system.suite;
/**
 Created by andrey on 26.05.16.
 */
import com.emc.mongoose.system.feature.core.DefaultWriteTest;
import com.emc.mongoose.system.feature.core.Read10BItemsTest;
import com.emc.mongoose.system.feature.core.Read10KBItemsTest;
import com.emc.mongoose.system.feature.core.Read10MBItemsTest;
import com.emc.mongoose.system.feature.core.Read200MBItemsTest;
import com.emc.mongoose.system.feature.loadtype.ReadVerificationTest;
import com.emc.mongoose.system.feature.core.ReadZeroSizeItemsTest;
import com.emc.mongoose.system.feature.core.WriteRandomSizedItemsTest;
import com.emc.mongoose.system.feature.core.WriteUsing100ConnTest;
import com.emc.mongoose.system.feature.core.WriteUsing10ConnTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	DefaultWriteTest.class,
	Read10BItemsTest.class,
	Read10KBItemsTest.class,
	Read10MBItemsTest.class,
	Read200MBItemsTest.class,
	ReadZeroSizeItemsTest.class,
	WriteRandomSizedItemsTest.class,
	WriteUsing10ConnTest.class,
	WriteUsing100ConnTest.class
})
public class CoreTestSuite {
}
