package com.emc.mongoose.ui.cli;

import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 Created by kurila on 24.08.16.
 */
public class CliArgParserTest {
	
	@Test
	public void parseTest() {

		final Map<String, String> argsMap = new HashMap<>();
		argsMap.put("--name", "goose");
		argsMap.put("--io-buffer-size", "1KB-4MB");
		argsMap.put("--storage-http-headers", "customHeaderName:customHeaderValue");
		
		final List<String> args = new ArrayList<>();
		for(final String argName : argsMap.keySet()) {
			args.add(argName + '=' + argsMap.get(argName));
		}
		final Map<String, Object> argTree = CliArgParser.parseArgs(null, args.toArray(new String[]{}));
		
		assertEquals(argsMap.get("--name"), argTree.get("name"));
		assertEquals(
			argsMap.get("--io-buffer-size"),
			((Map) ((Map) argTree.get("io")).get("buffer")).get("size")
		);
		assertEquals(
			argsMap.get("--storage-http-headers"),
			((Map) ((Map) argTree.get("storage")).get("http")).get("headers")
		);
	}
}
