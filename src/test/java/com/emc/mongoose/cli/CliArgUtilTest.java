package com.emc.mongoose.cli;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import java.util.Map;

public class CliArgUtilTest {

	@Test
	public final void test()
	throws Exception {
		final String[] args = new String[] {
			"--name=goose",
			"--io-buffer-size=1KB-4MB",
			"--storage-node-http-headers=customHeaderName:customHeaderValue",
			"--enable-some-feature",
		};
		final Map<String, String> parsedArgs = CliArgUtil.parseArgs(args);
		assertEquals("goose", parsedArgs.get("name"));
		assertEquals("1KB-4MB", parsedArgs.get("io-buffer-size"));
		assertEquals("customHeaderName:customHeaderValue", parsedArgs.get("storage-node-http-headers"));
		assertEquals(Boolean.TRUE.toString(), parsedArgs.get("enable-some-feature"));
	}

}
