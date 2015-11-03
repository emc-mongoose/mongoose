package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.11.15.
 */
import com.emc.mongoose.integ.cambridgelab.S3Read100MBTest;
import com.emc.mongoose.integ.cambridgelab.S3WriteByCountTest;
//
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	S3WriteByCountTest.class,
	S3Read100MBTest.class,
})
public class CambridgeLabDistributedTestSuite {
}
