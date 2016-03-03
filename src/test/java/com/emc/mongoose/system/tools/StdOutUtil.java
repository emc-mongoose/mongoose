package com.emc.mongoose.system.tools;
//
//
import java.io.PrintStream;
/**
 Created by kurila on 14.07.15.
 */
public abstract class StdOutUtil {
	//
	private final static PrintStream defaultStdOut = System.out;
	//
	public static BufferingOutputStream getStdOutBufferingStream() {
		defaultStdOut.flush();
		final BufferingOutputStream stdOutBufferingStream = new BufferingOutputStream(defaultStdOut);
		System.setOut(new PrintStream(stdOutBufferingStream));
		return stdOutBufferingStream;
	}
	//
	public static void reset() {
		System.setOut(defaultStdOut);
	}
}
