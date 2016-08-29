package com.emc.mongoose.ui.config;

import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;

/**
 Created by kurila on 24.08.16.
 */
public class ConfigTest {
	
	@Test
	public void testApply()
	throws Exception {
		
		final Map<String, String> argsMap = new HashMap<>();
		argsMap.put("--name", "goose");
		argsMap.put("--version", "1.2.5.10");
		argsMap.put("--io-buffer-size", "1KB-4MB");
		argsMap.put("--socket-timeoutMilliSec", "123456");
		argsMap.put("--socket-reuseAddr", "true");
		argsMap.put("--socket-tcpNoDelay", "false");
		argsMap.put("--socket-interestOpQueued", null);
		argsMap.put("--item-data-content-ringSize", "16MB");
		argsMap.put("--item-data-ranges", "1");
		argsMap.put("--load-limit-count", "1000");
		argsMap.put("--load-limit-rate", "12.345");
		argsMap.put("--load-limit-size", "321KB");
		argsMap.put("--load-limit-time", "5m");
		argsMap.put("--storage-addrs", "10.123.45.67,10.123.45.68,10.123.45.69,10.123.45.70");
		argsMap.put("--storage-http-fsAccess", "true");
		argsMap.put("--storage-http-headers", "customHeaderName:customHeaderValue");
		argsMap.put("--storage-mock-headCount", "2");
		
		final List<String> args = new ArrayList<>();
		String t;
		for(final String argName : argsMap.keySet()) {
			t = argsMap.get(argName);
			if(t == null) {
				args.add(argName);
			} else {
				args.add(argName + '=' + argsMap.get(argName));
			}
		}
		final Map<String, Object> argTree = CliArgParser.parseArgs(args.toArray(new String[]{}));
		
		final Config config = ConfigLoader.loadDefaultConfig();
		config.apply(argTree);
		
		assertEquals(argsMap.get("--name"), config.getName());
		assertEquals(argsMap.get("--version"), config.getVersion());
		assertEquals(
			new SizeInBytes(argsMap.get("--io-buffer-size")),
			config.getIoConfig().getBufferConfig().getSize()
		);
		assertEquals(
			Integer.parseInt(argsMap.get("--socket-timeoutMilliSec")),
			config.getSocketConfig().getTimeoutMilliSec()
		);
		assertEquals(true, config.getSocketConfig().getReuseAddr());
		assertEquals(false, config.getSocketConfig().getTcpNoDelay());
		assertEquals(true, config.getSocketConfig().getInterestOpQueued());
		assertEquals(
			"16MB",
			config.getItemConfig().getDataConfig().getContentConfig().getRingSize().toString()
		);
		assertEquals(1, config.getItemConfig().getDataConfig().getRanges().getRandomCount());
		assertEquals(1000, config.getLoadConfig().getLimitConfig().getCount());
		assertEquals(12.345, config.getLoadConfig().getLimitConfig().getRate());
		assertEquals("321KB", config.getLoadConfig().getLimitConfig().getSize().toString());
		assertEquals(300, config.getLoadConfig().getLimitConfig().getTime());
		assertEquals(4, config.getStorageConfig().getAddrs().size());
		assertEquals(true, config.getStorageConfig().getHttpConfig().getFsAccess());
		assertEquals(
			"customHeaderValue",
			config.getStorageConfig().getHttpConfig().getHeaders().get("customHeaderName")
		);
		assertEquals(2, config.getStorageConfig().getMockConfig().getHeadCount());
	}
	
	@Test
	public void testInvalidSizeValue()
	throws Exception {
		final Config config = ConfigLoader.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("io", new HashMap<String, Object>() {{
					put("buffer", new HashMap<String, Object>() {{
						put("size", "invalidSizeValue");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidTimeValue()
	throws Exception {
		final Config config = ConfigLoader.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("load", new HashMap<String, Object>() {{
					put("limit", new HashMap<String, Object>() {{
						put("time", "100500y");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidRangesValue()
	throws Exception {
		final Config config = ConfigLoader.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("item", new HashMap<String, Object>() {{
					put("data", new HashMap<String, Object>() {{
						put("ranges", "nope");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidInteger()
	throws Exception {
		final Config config = ConfigLoader.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("load", new HashMap<String, Object>() {{
					put("limit", new HashMap<String, Object>() {{
						put("count", "nope");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testNoSuchArgName()
	throws Exception {
		final Config config = ConfigLoader.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("load", new HashMap<String, Object>() {{
					put("limit", new HashMap<String, Object>() {{
						put("blabla", "123");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentNameException e) {
			Assert.assertEquals("--load-limit-blabla", e.getMessage());
		}
	}
}
