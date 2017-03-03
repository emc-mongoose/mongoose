package com.emc.mongoose.tests.unit;

import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig;
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
		argsMap.put("--test-step-name", "goose");
		argsMap.put("--version", "1.2.5.10");
		argsMap.put("--item-data-content-ringSize", "16MB");
		argsMap.put("--item-data-ranges-random", "1");
		argsMap.put("--test-step-limit-count", "1000");
		argsMap.put("--test-step-limit-rate", "12.345");
		argsMap.put("--test-step-limit-size", "321KB");
		argsMap.put("--test-step-limit-time", "5m");
		argsMap.put("--storage-net-timeoutMilliSec", "123456");
		argsMap.put("--storage-net-reuseAddr", "true");
		argsMap.put("--storage-net-tcpNoDelay", "false");
		argsMap.put("--storage-net-interestOpQueued", null);
		argsMap.put("--storage-net-node-addrs", "10.123.45.67,10.123.45.68,10.123.45.69,10.123.45.70");
		argsMap.put("--storage-net-http-fsAccess", "true");
		argsMap.put("--storage-net-http-headers", "customHeaderName:customHeaderValue");
		
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
		final NetConfig netConfig = config.getStorageConfig().getNetConfig();
		assertEquals(
			Integer.parseInt(argsMap.get("--storage-net-timeoutMilliSec")),
			netConfig.getTimeoutMilliSec()
		);
		assertEquals(true, netConfig.getReuseAddr());
		assertEquals(false, netConfig.getTcpNoDelay());
		assertEquals(true, netConfig.getInterestOpQueued());
		final Config.ItemConfig.DataConfig dataConfig = config.getItemConfig().getDataConfig();
		assertEquals(
			"16MB",
			dataConfig.getContentConfig().getRingSize().toString()
		);
		assertEquals(1, dataConfig.getRangesConfig().getRandom());
		final Config.TestConfig.StepConfig.LimitConfig limitConfig = config.getTestConfig().getStepConfig().getLimitConfig();
		assertEquals(1000, limitConfig.getCount());
		assertEquals(12.345, limitConfig.getRate());
		assertEquals("321KB", limitConfig.getSize().toString());
		assertEquals(300, limitConfig.getTime());
		assertEquals(4, netConfig.getNodeConfig().getAddrs().size());
		assertEquals(true, netConfig.getHttpConfig().getFsAccess());
		assertEquals(
			"customHeaderValue",
			netConfig.getHttpConfig().getHeaders().get("customHeaderName")
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
				put("test", new HashMap<String, Object>() {{
					put("step", new HashMap<String, Object>() {{
						put("limit", new HashMap<String, Object>() {{
							put("time", "100500y");
						}});
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
				put("test", new HashMap<String, Object>() {{
					put("step", new HashMap<String, Object>() {{
						put("limit", new HashMap<String, Object>() {{
							put("count", "nope");
						}});
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
				put("storage", new HashMap<String, Object>() {{
					put("driver", new HashMap<String, Object>() {{
						put("blabla", "123");
					}});
				}});
			}});
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentNameException e) {
			Assert.assertEquals("--storage-driver-blabla", e.getMessage());
		}
	}
}
