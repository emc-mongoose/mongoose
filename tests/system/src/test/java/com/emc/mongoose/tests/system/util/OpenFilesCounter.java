package com.emc.mongoose.tests.system.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 Created by andrey on 04.06.17.
 */
public interface OpenFilesCounter {

	static List<String> getOutputLines(final String path)
	throws IOException {
		final String[] cmd = { "lsof", path };
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
		final List<String> lines = getOutputLines(path);
		if(lines.size() > 0) {
			return Integer.parseInt(lines.get(0));
		} else {
			throw new IllegalArgumentException();
		}
	}
}
