package com.emc.mongoose.tests.system.util;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by olga on 08.07.15.
 */
public final class PortListener {
	
	public static Scanner getNetstatOutput()
	throws IOException {
		final String[] netstatCommand = { "netstat", "-an" };
		final Process netstatProcess = Runtime.getRuntime().exec(netstatCommand);
		return new Scanner(netstatProcess.getInputStream(), "IBM850").useDelimiter("\\n");
	}
	
	public static int getCountConnectionsOnPort(final String port)
	throws IOException {
		int countConnections = 0;
		try(final Scanner netstatOutputScanner = getNetstatOutput()) {
			String line;
			while(netstatOutputScanner.hasNext()) {
				line = netstatOutputScanner.next();
				if(line.contains(port)) {
					countConnections ++;
				}
			}
		}
		return countConnections;
	}
}
