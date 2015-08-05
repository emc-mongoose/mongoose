package com.emc.mongoose.integ.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

/**
 * Created by olga on 08.07.15.
 */
public final class PortListener {

	public static int getCountConnectionsOnPort(final String port)
	throws Exception {
		int countConnections = 0;
		try (final BufferedReader bufferedReader = getNetstatOutput()) {
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				System.out.println(line);
			}
		}
		return 1;
	}

	private static BufferedReader getNetstatOutput()
	throws Exception {
		final String cmd = "netstat -an | grep \\:902 | wc -l";
		final Process netstat = Runtime.getRuntime().exec(cmd);
		netstat.waitFor();
		return new BufferedReader(
			new InputStreamReader(netstat.getInputStream())
		);
	}

}
