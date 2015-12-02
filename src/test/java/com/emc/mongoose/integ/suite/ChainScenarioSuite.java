package com.emc.mongoose.integ.suite;
/**
 Created by kurila on 02.12.15.
 */
import com.emc.mongoose.integ.feature.chain.ConcurrentChainCRUDTest;
import com.emc.mongoose.integ.feature.chain.CustomChainScenarioTest;
import com.emc.mongoose.integ.feature.chain.DefaultChainScenarioTest;
import com.emc.mongoose.integ.feature.chain.SequentialChainCRUDTest;
import com.emc.mongoose.integ.feature.chain.SequentialDistributedLoadTest;
import com.emc.mongoose.integ.feature.chain.SimultaneousDistributedLoadTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
@RunWith(Suite.class)
@Suite.SuiteClasses({
	ConcurrentChainCRUDTest.class,
	CustomChainScenarioTest.class,
	DefaultChainScenarioTest.class,
	SequentialChainCRUDTest.class,
	SequentialDistributedLoadTest.class,
	SimultaneousDistributedLoadTest.class,
})
public class ChainScenarioSuite {
}
