package com.emc.mongoose.integ;
//
import com.emc.mongoose.integ.tools.SavedOutputStream;
//
import org.junit.AfterClass;
import org.junit.BeforeClass;
//
import java.io.PrintStream;
/**
 Created by kurila on 14.07.15.
 */
public abstract class StdOutInterceptingTestBase {
	//
	protected static SavedOutputStream STD_OUT_INTERCEPT_STREAM;
	//
	@BeforeClass
	public static void before()
	throws Exception {
		STD_OUT_INTERCEPT_STREAM = new SavedOutputStream(System.out);
		System.setOut(new PrintStream(STD_OUT_INTERCEPT_STREAM));
	}
	//
	@AfterClass
	public static void after()
	throws Exception {
		System.setOut(STD_OUT_INTERCEPT_STREAM.getReplacedStream());
	}
}
