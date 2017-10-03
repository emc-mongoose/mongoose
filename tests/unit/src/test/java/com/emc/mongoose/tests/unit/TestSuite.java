package com.emc.mongoose.tests.unit;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	BufferingInputBaseTest.class,
	CliArgParserTest.class,
	ConfigTest.class,
	FileStorageDriverTest.class,
	ThreadLocalByteBufferTest.class,
	ValidateConfigTest.class,
	ValidateScenariosTest.class,
})
public class TestSuite {
}
