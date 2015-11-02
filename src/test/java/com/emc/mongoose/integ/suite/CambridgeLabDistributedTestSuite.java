package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.11.15.
 */
import com.emc.mongoose.integ.cambridgelab.WriteByCountTest;
//
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	WriteByCountTest.class,
})
public class CambridgeLabDistributedTestSuite {
}
