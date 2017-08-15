package com.emc.mongoose.tests.system.util;

import java.io.IOException;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by olga on 08.07.15.
 */
public interface PortTools {

	String FMT_PATTERN_CONN = "\\s+ESTABLISHED";
	
	static Scanner getNetstatOutput()
	throws IOException {
		final String[] netstatCommand = { "netstat", "-an" };
		final Process netstatProcess = Runtime.getRuntime().exec(netstatCommand);
		return new Scanner(netstatProcess.getInputStream(), "IBM850").useDelimiter("\\n");
	}
	
	static int getCountConnectionsOnPort(final String nodeAddrWithPort)
	throws IOException {
		int countConnections = 0;
		final Pattern patternConn = Pattern.compile(nodeAddrWithPort + FMT_PATTERN_CONN);
		try(final Scanner netstatOutputScanner = getNetstatOutput()) {
			String line;
			Matcher m;
			while(netstatOutputScanner.hasNext()) {
				line = netstatOutputScanner.next();
				m = patternConn.matcher(line);
				if(m.find()) {
					countConnections ++;
				}
			}
		}
		return countConnections;
	}

	static void killConnectionsOnPort(final int port)
	throws IOException, InterruptedException {
		final String[] cmd = { "fuser", "-k", Integer.toString(port) + "/tcp" };
		final Process process = Runtime.getRuntime().exec(cmd);
		process.waitFor();
	}
}
