package com.emc.mongoose.ui.config;

import com.emc.mongoose.ui.config.test.step.StepConfig;
import com.emc.mongoose.ui.config.test.step.node.NodeConfig;
import com.emc.mongoose.ui.config.util.ConfigMatcher;
import com.emc.mongoose.ui.config.util.ConfigNullMatcher;
import com.github.akurilov.commons.system.SizeInBytes;
import com.emc.mongoose.api.common.TimeUtil;
import com.emc.mongoose.ui.cli.CliArgParser;
import com.emc.mongoose.ui.config.item.ItemConfig;
import com.emc.mongoose.ui.config.item.data.DataConfig;
import com.emc.mongoose.ui.config.item.input.InputConfig;
import com.emc.mongoose.ui.config.item.naming.NamingConfig;
import com.emc.mongoose.ui.config.load.LoadConfig;
import com.emc.mongoose.ui.config.load.generator.GeneratorConfig;
import com.emc.mongoose.ui.config.output.metrics.MetricsConfig;
import com.emc.mongoose.ui.config.storage.StorageConfig;
import com.emc.mongoose.ui.config.storage.auth.AuthConfig;
import com.emc.mongoose.ui.config.storage.driver.DriverConfig;
import com.emc.mongoose.ui.config.storage.net.NetConfig;
import com.emc.mongoose.ui.config.storage.net.http.HttpConfig;
import com.emc.mongoose.ui.config.test.scenario.ScenarioConfig;
import com.emc.mongoose.ui.config.test.step.limit.LimitConfig;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 Created by kurila on 24.08.16.
 */
public class ConfigTest {

	@Test
	public void shouldParseWithoutFireballsThrowing()
	throws IOException {
		final Config config = Config.loadDefaults();
		MatcherAssert.assertThat(config, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(config.getVersion(), ConfigMatcher.equalTo("4.0.0", "version"));
		final NetConfig netConfig = config.getStorageConfig().getNetConfig();
		MatcherAssert.assertThat(netConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(netConfig.getTimeoutMilliSec(), ConfigMatcher.equalTo(0, "storage.net.timeoutMilliSec"));
		MatcherAssert.assertThat(netConfig.getReuseAddr(), ConfigMatcher.equalTo(true, "storage.net.reuseAddr"));
		MatcherAssert.assertThat(netConfig.getKeepAlive(), ConfigMatcher.equalTo(true, "storage.net.keepAlive"));
		MatcherAssert.assertThat(netConfig.getTcpNoDelay(), ConfigMatcher.equalTo(true, "storage.net.tcpNoDelay"));
		MatcherAssert.assertThat(netConfig.getLinger(), ConfigMatcher.equalTo(0, "storage.net.linger"));
		MatcherAssert.assertThat(netConfig.getBindBacklogSize(), ConfigMatcher.equalTo(0, "storage.net.bindBacklogSize"));
		MatcherAssert.assertThat(netConfig.getInterestOpQueued(), ConfigMatcher.equalTo(false, "storage.net.interestOpQueued"));
		final ItemConfig itemConfig = config.getItemConfig();
		MatcherAssert.assertThat(itemConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(itemConfig.getType(), ConfigMatcher.equalTo("data", "item.type"));
		final DataConfig dataConfig = itemConfig.getDataConfig();
		MatcherAssert.assertThat(dataConfig, ConfigNullMatcher.notNullValue());
		final com.emc.mongoose.ui.config.item.data.input.InputConfig
			dataInputConfig = dataConfig.getInputConfig();
		MatcherAssert.assertThat(dataInputConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(dataInputConfig.getFile(), ConfigNullMatcher.nullValue("item.data.content.file"));
		MatcherAssert.assertThat(dataInputConfig.getSeed(), ConfigMatcher.equalTo("7a42d9c483244167", "item.data.content.seed"));
		MatcherAssert.assertThat(
			dataInputConfig.getLayerConfig().getSize(),
			ConfigMatcher.equalTo(new SizeInBytes("4MB"), "item.data.content.ringSize")
		);
		MatcherAssert.assertThat(dataConfig.getRangesConfig().getRandom(), ConfigMatcher.equalTo(0, "item.data.ranges.random"));
		MatcherAssert.assertThat(dataConfig.getSize(), ConfigMatcher.equalTo(new SizeInBytes("1MB"), "item.data.size"));
		MatcherAssert.assertThat(dataConfig.getVerify(), ConfigMatcher.equalTo(false, "item.data.verify"));
		final InputConfig itemInputConfig = itemConfig.getInputConfig();
		MatcherAssert.assertThat(itemInputConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(itemInputConfig.getPath(), ConfigNullMatcher.nullValue("item.input.path"));
		MatcherAssert.assertThat(itemInputConfig.getFile(), ConfigNullMatcher.nullValue("item.input.file"));
		final com.emc.mongoose.ui.config.item.output.OutputConfig
			outputConfig = itemConfig.getOutputConfig();
		MatcherAssert.assertThat(outputConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(outputConfig.getPath(), ConfigNullMatcher.nullValue("item.output.path"));
		MatcherAssert.assertThat(outputConfig.getFile(), ConfigNullMatcher.nullValue("item.output.file"));
		final NamingConfig namingConfig = itemConfig.getNamingConfig();
		MatcherAssert.assertThat(namingConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(namingConfig.getType(), ConfigMatcher.equalTo("random", "item.naming.type"));
		MatcherAssert.assertThat(namingConfig.getPrefix(), ConfigNullMatcher.nullValue("item.naming.prefix"));
		MatcherAssert.assertThat(namingConfig.getRadix(), ConfigMatcher.equalTo(36, "item.naming.radix"));
		MatcherAssert.assertThat(namingConfig.getOffset(), ConfigMatcher.equalTo(0L, "item.naming.offset"));
		MatcherAssert.assertThat(namingConfig.getLength(), ConfigMatcher.equalTo(12, "item.naming.length"));
		final LoadConfig loadConfig = config.getLoadConfig();
		MatcherAssert.assertThat(loadConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(loadConfig.getGeneratorConfig().getRecycleConfig().getEnabled(), ConfigMatcher

			.equalTo(false, "load.circular"));
		MatcherAssert.assertThat(loadConfig.getType(), ConfigMatcher.equalTo("create", "load.type" +
			""));
		MatcherAssert.assertThat(config.getLoadConfig().getLimitConfig().getConcurrency(), ConfigMatcher
			.equalTo(1, "load.concurrency"));
		final LimitConfig limitConfig = config.getTestConfig().getStepConfig().getLimitConfig();
		MatcherAssert.assertThat(limitConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(limitConfig.getCount(), ConfigMatcher.equalTo(0L, "load.limit.count"));
		MatcherAssert.assertThat(loadConfig.getLimitConfig().getRate(), ConfigMatcher.equalTo(0.0, "load.limit.rate"));
		MatcherAssert.assertThat(limitConfig.getSize(), ConfigMatcher.equalTo(new SizeInBytes(0), "load.limit.size"));
		final String timeTestValue = "0s";
		MatcherAssert.assertThat(
			limitConfig.getTime(),
			ConfigMatcher.equalTo(
				TimeUtil
					.getTimeUnit(timeTestValue)
					.toSeconds(TimeUtil.getTimeValue(timeTestValue)),
				"load.limit.time"
			)
		);
		final GeneratorConfig generatorConfig = loadConfig.getGeneratorConfig();
		MatcherAssert.assertThat(generatorConfig.getRemote(), ConfigMatcher.equalTo(false, "load.generator.remote"));
		final MetricsConfig metricsConfig = config.getOutputConfig().getMetricsConfig();
		MatcherAssert.assertThat(metricsConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(metricsConfig.getThreshold(), ConfigMatcher.equalTo(0.0, "load.metrics.intermediate"));
		final String periodTestValue = "10s";
		MatcherAssert.assertThat(
			metricsConfig.getAverageConfig().getPeriod(),
			ConfigMatcher.equalTo(
				TimeUtil
					.getTimeUnit(periodTestValue)
					.toSeconds(TimeUtil.getTimeValue(periodTestValue)),
				"load.metrics.period"
			)
		);
		final ScenarioConfig scenarioConfig = config.getTestConfig().getScenarioConfig();
		MatcherAssert.assertThat(scenarioConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(scenarioConfig.getFile(), ConfigNullMatcher.nullValue("run.file"));
		final StorageConfig storageConfig = config.getStorageConfig();
		MatcherAssert.assertThat(storageConfig, ConfigNullMatcher.notNullValue());
		final AuthConfig authConfig = storageConfig.getAuthConfig();
		MatcherAssert.assertThat(authConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(authConfig.getUid(), ConfigNullMatcher.nullValue("storage.auth.uid"));
		MatcherAssert.assertThat(authConfig.getSecret(), ConfigNullMatcher.nullValue("storage.auth" +
			".secret"));
		MatcherAssert.assertThat(authConfig.getToken(), ConfigNullMatcher.nullValue("storage.auth.token"));
		final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
		MatcherAssert.assertThat(httpConfig, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(httpConfig.getFsAccess(), ConfigMatcher.equalTo(false, "storage.net.http.fsAccess"));
		final Map<String, String> headers = httpConfig.getHeaders();
		MatcherAssert.assertThat(headers, ConfigNullMatcher.notNullValue());
		MatcherAssert.assertThat(headers.containsKey(HttpConfig.KEY_HEADER_CONNECTION),
			ConfigMatcher.equalTo(true, "storage.net.http.headers[Connection]"));
		MatcherAssert.assertThat(headers.get(HttpConfig.KEY_HEADER_CONNECTION),
			ConfigMatcher.equalTo("Keep-Alive", "storage.net.http.headers[Connection]"));
		MatcherAssert.assertThat(headers.containsKey(HttpConfig.KEY_HEADER_USER_AGENT),
			ConfigMatcher.equalTo(true, "storage.net.http.headers[User-Agent]"));
		MatcherAssert.assertThat(headers.get(HttpConfig.KEY_HEADER_USER_AGENT),
			ConfigMatcher.equalTo("mongoose/4.0.0", "storage.net.http.headers[User-Agent]"));
		MatcherAssert.assertThat(httpConfig.getNamespace(), ConfigNullMatcher.nullValue("storage" +
			".net.http.namespace"));
		MatcherAssert.assertThat(httpConfig.getVersioning(), ConfigMatcher.equalTo(false,
			"storage.net.http.versioning"));
		MatcherAssert.assertThat(
			storageConfig.getNetConfig().getNodeConfig().getAddrs().get(0),
			ConfigMatcher.equalTo("127.0.0.1", "storage.net.node.addrs")
		);
		final NodeConfig nodeConfig = config.getTestConfig().getStepConfig().getNodeConfig();
		assertThat(
			nodeConfig.getAddrs().get(0),
			ConfigMatcher.equalTo("127.0.0.1", "storage.driver.addrs")
		);
		MatcherAssert.assertThat(storageConfig.getNetConfig().getNodeConfig().getPort(), ConfigMatcher
			.equalTo(9020, "storage.port"));
		MatcherAssert.assertThat(storageConfig.getNetConfig().getSsl(), ConfigMatcher.equalTo(false, "storage.net.ssl."));
	}
	
	@Test
	public void testApply()
	throws Exception {
		
		final Map<String, String> argsMap = new HashMap<>();
		argsMap.put("--test-step-id", "goose");
		argsMap.put("--version", "1.2.5.10");
		argsMap.put("--item-data-input-layer-size", "16MB");
		argsMap.put("--item-data-ranges-random", "1");
		argsMap.put("--test-step-limit-count", "1000");
		argsMap.put("--load-rate-limit", "12.345");
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
		
		final Config config = Config.loadDefaults();
		config.apply(argTree, null);
		
		assertEquals(argsMap.get("--version"), config.getVersion());
		final NetConfig netConfig = config.getStorageConfig().getNetConfig();
		assertEquals(
			Integer.parseInt(argsMap.get("--storage-net-timeoutMilliSec")),
			netConfig.getTimeoutMilliSec()
		);
		assertEquals(true, netConfig.getReuseAddr());
		assertEquals(false, netConfig.getTcpNoDelay());
		assertEquals(true, netConfig.getInterestOpQueued());
		final DataConfig dataConfig = config.getItemConfig().getDataConfig();
		assertEquals(
			"16MB",
			dataConfig.getInputConfig().getLayerConfig().getSize().toString()
		);
		assertEquals(1, dataConfig.getRangesConfig().getRandom());
		final LimitConfig limitConfig = config.getTestConfig().getStepConfig().getLimitConfig();
		assertEquals(1000, limitConfig.getCount());
		assertEquals(12.345, config.getLoadConfig().getLimitConfig().getRate());
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
		final Config config = Config.loadDefaults();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("item", new HashMap<String, Object>() {{
					put("data", new HashMap<String, Object>() {{
						put("size", "invalidSizeValue");
					}});
				}});
			}}, null);
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidTimeValue()
	throws Exception {
		final Config config = Config.loadDefaults();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("test", new HashMap<String, Object>() {{
					put("step", new HashMap<String, Object>() {{
						put("limit", new HashMap<String, Object>() {{
							put("time", "100500y");
						}});
					}});
				}});
			}}, null);
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidRangesValue()
	throws Exception {
		final Config config = Config.loadDefaults();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("item", new HashMap<String, Object>() {{
					put("data", new HashMap<String, Object>() {{
						put("ranges", new HashMap<String, Object>() {{
							put("random", "nope");
						}});
					}});
				}});
			}}, null);
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testInvalidInteger()
	throws Exception {
		final Config config = Config.loadDefaults();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("test", new HashMap<String, Object>() {{
					put("step", new HashMap<String, Object>() {{
						put("limit", new HashMap<String, Object>() {{
							put("count", "nope");
						}});
					}});
				}});
			}}, null);
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentException e) {
		}
	}
	
	@Test
	public void testNoSuchArgName()
	throws Exception {
		final Config config = Config.loadDefaults();
		try {
			config.apply(new HashMap<String, Object>() {{
				put("storage", new HashMap<String, Object>() {{
					put("driver", new HashMap<String, Object>() {{
						put("blabla", "123");
					}});
				}});
			}}, null);
			Assert.fail("No exception thrown");
		} catch(final IllegalArgumentNameException e) {
			Assert.assertEquals("--storage-driver-blabla", e.getMessage());
		}
	}
}
