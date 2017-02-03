package com.emc.mongoose.tests.unit;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.IllegalArgumentNameException;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
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
		argsMap.put("--load-job-name", "goose");
		argsMap.put("--version", "1.2.5.10");
		argsMap.put("--socket-timeoutMilliSec", "123456");
		argsMap.put("--socket-reuseAddr", "true");
		argsMap.put("--socket-tcpNoDelay", "false");
		argsMap.put("--socket-interestOpQueued", null);
		argsMap.put("--item-data-content-ringSize", "16MB");
		argsMap.put("--item-data-ranges-random", "1");
		argsMap.put("--load-limit-count", "1000");
		argsMap.put("--load-limit-rate", "12.345");
		argsMap.put("--load-limit-size", "321KB");
		argsMap.put("--load-limit-time", "5m");
		argsMap.put("--storage-node-addrs", "10.123.45.67,10.123.45.68,10.123.45.69,10.123.45.70");
		argsMap.put("--storage-http-fsAccess", "true");
		argsMap.put("--storage-http-headers", "customHeaderName:customHeaderValue");
		
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
		final Map<String, Object> argTree = CliArgParser.parseArgs(null, args.toArray(new String[]{}));
		
		final Config config = ConfigParser.loadDefaultConfig();
		config.apply(argTree);
		
		assertEquals(argsMap.get("--version"), config.getVersion());
		final Config.SocketConfig socketConfig = config.getSocketConfig();
		assertEquals(
			Integer.parseInt(argsMap.get("--socket-timeoutMilliSec")),
			socketConfig.getTimeoutMilliSec()
		);
		assertEquals(true, socketConfig.getReuseAddr());
		assertEquals(false, socketConfig.getTcpNoDelay());
		assertEquals(true, socketConfig.getInterestOpQueued());
		final Config.ItemConfig.DataConfig dataConfig = config.getItemConfig().getDataConfig();
		assertEquals(
			"16MB",
			dataConfig.getContentConfig().getRingSize().toString()
		);
		assertEquals(1, dataConfig.getRangesConfig().getRandom());
		final Config.LoadConfig loadConfig = config.getLoadConfig();
		assertEquals(1000, loadConfig.getLimitConfig().getCount());
		assertEquals(12.345, loadConfig.getLimitConfig().getRate());
		assertEquals("321KB", loadConfig.getLimitConfig().getSize().toString());
		assertEquals(300, loadConfig.getLimitConfig().getTime());
		final Config.StorageConfig storageConfig = config.getStorageConfig();
		assertEquals(4, storageConfig.getNodeConfig().getAddrs().size());
		assertEquals(true, storageConfig.getHttpConfig().getFsAccess());
		assertEquals(
			"customHeaderValue",
			storageConfig.getHttpConfig().getHeaders().get("customHeaderName")
		);
	}
	
	@Test
	public void testInvalidSizeValue()
	throws Exception {
		final Config config = ConfigParser.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("item", new HashMap<String, Object>() {{
					put("data", new HashMap<String, Object>() {{
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
		final Config config = ConfigParser.loadDefaultConfig();
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
		final Config config = ConfigParser.loadDefaultConfig();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("item", new HashMap<String, Object>() {{
					put("data", new HashMap<String, Object>() {{
						put("ranges", new HashMap<String, Object>() {{
							put("random", "nope");
						}});
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
		final Config config = ConfigParser.loadDefaultConfig();
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
		final Config config = ConfigParser.loadDefaultConfig();
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
