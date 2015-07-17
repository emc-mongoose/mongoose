package com.emc.mongoose.integ.suite;
//
import com.emc.mongoose.integ.tools.BufferingOutputStream;
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
public abstract class StdOutInterceptorTestSuite {
	//
	private final static PrintStream defaultStdOut = System.out;
	//
	public static BufferingOutputStream getStdOutBufferingStream() {
		final BufferingOutputStream stdOutBufferingStream = new BufferingOutputStream(defaultStdOut);
		System.setOut(new PrintStream(stdOutBufferingStream));
		return stdOutBufferingStream;
	}
	//
	public static void reset() {
		System.setOut(defaultStdOut);
	}
	//
	@BeforeClass
	public static void setUpClass()
	throws Exception {
	}
	//
	@AfterClass
	public static void tearDownClass()
	throws Exception {
		reset();
	}
}
