package com.emc.mongoose.core.api.persist;
/**
 Created by kurila on 26.12.14.
 */
public interface ConsoleColors {
	String
		RESET = "\u001B[0m",
		BLACK = "\u001B[30m",
		RED = "\u001B[31m",
		GREEN = "\u001B[32m",
		YELLOW = "\u001B[33m",
		BLUE = "\u001B[34m",
		PURPLE = "\u001B[35m",
		CYAN = "\u001B[36m",
		WHITE = "\u001B[37m",
		//
		INT_YELLOW_OVER_GREEN = YELLOW + "%d" + GREEN,
		INT_RED_OVER_GREEN = RED + "%d" + GREEN;
}
