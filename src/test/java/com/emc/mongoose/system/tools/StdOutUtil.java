package com.emc.mongoose.system.tools;

import java.io.PrintStream;
/**
 Created by kurila on 14.07.15.
 */
public abstract class StdOutUtil {
	//
	public static BufferingOutputStream getStdOutBufferingStream() {
		System.out.flush();
		final BufferingOutputStream stdOutBufferingStream = new BufferingOutputStream(System.out);
		System.setOut(new PrintStream(stdOutBufferingStream));
		return stdOutBufferingStream;
	}
	//
	public static void reset() {
		System.setOut(System.out);
	}
}
