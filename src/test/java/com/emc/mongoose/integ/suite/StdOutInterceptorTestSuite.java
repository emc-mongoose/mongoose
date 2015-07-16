package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.integ.tools.SavedOutputStream;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
//
import java.io.PrintStream;
/**
 Created by kurila on 14.07.15.
 */
@RunWith(Suite.class)
public abstract class StdOutInterceptorTestSuite
extends LoggingTestSuite {
	//
	public static SavedOutputStream STD_OUT_INTERCEPT_STREAM;
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
		LoggingTestSuite.setUpClass();
		STD_OUT_INTERCEPT_STREAM = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(STD_OUT_INTERCEPT_STREAM));
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		System.setOut(STD_OUT_INTERCEPT_STREAM.getReplacedStream());
		LoggingTestSuite.tearDownClass();
	}
}
