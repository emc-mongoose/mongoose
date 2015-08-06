package com.emc.mongoose.integ.tools;

import java.io.IOException;
import java.util.Scanner;

/**
 * Created by olga on 08.07.15.
 */
public final class PortListener {

	public static Scanner getNetstatOutput()
	throws IOException {
		final String[] netstatCommand = { "netstat", "-an" };
		final Process netstat = Runtime.getRuntime().exec(netstatCommand);
		return new Scanner(netstat.getInputStream(), "IBM850").useDelimiter("\\n");
	}

	public static int getCountConnectionsOnPort(final String port)
	throws IOException {
		Scanner netstatOutputScanner = getNetstatOutput();
		int countConnections = 0;
		String line;
		while (netstatOutputScanner.hasNext()) {
			line = netstatOutputScanner.next();
			if (line.contains(port)) {
				countConnections++;
			}
		}
		netstatOutputScanner.close();
		return countConnections;
	}
}
