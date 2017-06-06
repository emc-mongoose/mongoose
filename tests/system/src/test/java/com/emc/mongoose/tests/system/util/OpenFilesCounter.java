package com.emc.mongoose.tests.system.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 Created by andrey on 04.06.17.
 */
public interface OpenFilesCounter {

	static List<String> getLsofOutputLines(final String path)
	throws IOException {
		final String[] cmd = { "/bin/sh", "-c", "lsof -b | grep " + path };
		final Process p = Runtime.getRuntime().exec(cmd);
		final List<String> lines = new ArrayList<>();
		try(
			final Scanner s = new Scanner(
				p.getInputStream(), "IBM850").useDelimiter(System.lineSeparator()
			)
		) {
			while(s.hasNext()) {
				lines.add(s.next());
			}
		}
		return lines;
	}

	static int getOpenFilesCount(final String path)
	throws IOException, NumberFormatException, IllegalArgumentException {
		final List<String> lines = getLsofOutputLines(path);
		final int linesCount = lines.size();
		if(linesCount > 0) {
			for(int i = linesCount - 1; i >= 0; i --) {
				try {
					return Integer.parseInt(lines.get(0));
				} catch(final NumberFormatException e) {
					continue;
				}
			}
		}
		return 0;
	}
}
