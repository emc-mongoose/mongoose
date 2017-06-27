package com.emc.mongoose.tests.unit;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.common.api.SizeInBytes;
import com.emc.mongoose.common.api.TimeUtil;
import static com.emc.mongoose.ui.config.Config.ItemConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.DataConfig.ContentConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.InputConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.NamingConfig;
import static com.emc.mongoose.ui.config.Config.ItemConfig.OutputConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig;
import static com.emc.mongoose.ui.config.Config.LoadConfig.GeneratorConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.StepConfig.LimitConfig;
import static com.emc.mongoose.ui.config.Config.OutputConfig.MetricsConfig;
import static com.emc.mongoose.ui.config.Config.TestConfig.ScenarioConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.AuthConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.DriverConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.MockConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig;
import static com.emc.mongoose.ui.config.Config.StorageConfig.NetConfig.HttpConfig;
import com.emc.mongoose.ui.config.reader.jackson.ConfigParser;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static com.emc.mongoose.tests.unit.util.ConfigMatcher.equalTo;
import static com.emc.mongoose.tests.unit.util.ConfigNullMatcher.notNullValue;
import static com.emc.mongoose.tests.unit.util.ConfigNullMatcher.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 Created by kurila on 14.07.16.
 */
public class ConfigParserTest {

	@Test
	public void shouldParseWithoutFireballsThrowing()
	throws IOException {
		final Config config = ConfigParser.loadDefaultConfig();
		assertThat(config, notNullValue());
		assertThat(config.getVersion(), equalTo("3.5.0", "version"));
		final NetConfig netConfig = config.getStorageConfig().getNetConfig();
		assertThat(netConfig, notNullValue());
		assertThat(netConfig.getTimeoutMilliSec(), equalTo(0, "storage.net.timeoutMilliSec"));
		assertThat(netConfig.getReuseAddr(), equalTo(true, "storage.net.reuseAddr"));
		assertThat(netConfig.getKeepAlive(), equalTo(true, "storage.net.keepAlive"));
		assertThat(netConfig.getTcpNoDelay(), equalTo(true, "storage.net.tcpNoDelay"));
		assertThat(netConfig.getLinger(), equalTo(0, "storage.net.linger"));
		assertThat(netConfig.getBindBacklogSize(), equalTo(0, "storage.net.bindBacklogSize"));
		assertThat(netConfig.getInterestOpQueued(), equalTo(false, "storage.net.interestOpQueued"));
		final ItemConfig itemConfig = config.getItemConfig();
		assertThat(itemConfig, notNullValue());
		assertThat(itemConfig.getType(), equalTo("data", "item.type"));
		final DataConfig dataConfig = itemConfig.getDataConfig();
		assertThat(dataConfig, notNullValue());
		final ContentConfig contentConfig = dataConfig.getContentConfig();
		assertThat(contentConfig, notNullValue());
		assertThat(contentConfig.getFile(), nullValue("item.data.content.file"));
		assertThat(contentConfig.getSeed(), equalTo("7a42d9c483244167", "item.data.content.seed"));
		assertThat(
			contentConfig.getRingConfig().getSize(),
			equalTo(new SizeInBytes("4MB"), "item.data.content.ringSize")
		);
		assertThat(dataConfig.getRangesConfig().getRandom(), equalTo(0, "item.data.ranges.random"));
		assertThat(dataConfig.getSize(), equalTo(new SizeInBytes("1MB"), "item.data.size"));
		assertThat(dataConfig.getVerify(), equalTo(false, "item.data.verify"));
		final InputConfig inputConfig = itemConfig.getInputConfig();
		assertThat(inputConfig, notNullValue());
		assertThat(inputConfig.getPath(), nullValue("item.input.path"));
		assertThat(inputConfig.getFile(), nullValue("item.input.file"));
		final OutputConfig outputConfig= itemConfig.getOutputConfig();
		assertThat(outputConfig, notNullValue());
		assertThat(outputConfig.getPath(), nullValue("item.output.path"));
		assertThat(outputConfig.getFile(), nullValue("item.output.file"));
		final NamingConfig namingConfig = itemConfig.getNamingConfig();
		assertThat(namingConfig, notNullValue());
		assertThat(namingConfig.getType(), equalTo("random", "item.naming.type"));
		assertThat(namingConfig.getPrefix(), nullValue("item.naming.prefix"));
		assertThat(namingConfig.getRadix(), equalTo(36, "item.naming.radix"));
		assertThat(namingConfig.getOffset(), equalTo(0L, "item.naming.offset"));
		assertThat(namingConfig.getLength(), equalTo(13, "item.naming.length"));
		final LoadConfig loadConfig = config.getLoadConfig();
		assertThat(loadConfig, notNullValue());
		assertThat(loadConfig.getCircular(), equalTo(false, "load.circular"));
		assertThat(loadConfig.getType(), equalTo("create", "load.type"));
		assertThat(config.getStorageConfig().getDriverConfig().getConcurrency(), equalTo(1, "load.concurrency"));
		final LimitConfig limitConfig = config.getTestConfig().getStepConfig().getLimitConfig();
		assertThat(limitConfig, notNullValue());
		assertThat(limitConfig.getCount(), equalTo(0L, "load.limit.count"));
		assertThat(limitConfig.getRate(), equalTo(0.0, "load.limit.rate"));
		assertThat(limitConfig.getSize(), equalTo(new SizeInBytes(0), "load.limit.size"));
		final String timeTestValue = "0s";
		assertThat(
			limitConfig.getTime(),
			equalTo(
				TimeUtil
					.getTimeUnit(timeTestValue)
					.toSeconds(TimeUtil.getTimeValue(timeTestValue)),
				"load.limit.time"
			)
		);
		final GeneratorConfig generatorConfig = loadConfig.getGeneratorConfig();
		assertThat(generatorConfig.getRemote(), equalTo(false, "load.generator.remote"));
		assertThat(
			generatorConfig.getAddrs().get(0), equalTo("127.0.0.1", "load.generator.addrs")
		);
		final MetricsConfig metricsConfig = config.getOutputConfig().getMetricsConfig();
		assertThat(metricsConfig, notNullValue());
		assertThat(metricsConfig.getThreshold(), equalTo(0.0, "load.metrics.intermediate"));
		final String periodTestValue = "10s";
		assertThat(
			metricsConfig.getAverageConfig().getPeriod(),
			equalTo(
				TimeUtil
					.getTimeUnit(periodTestValue)
					.toSeconds(TimeUtil.getTimeValue(periodTestValue)),
				"load.metrics.period"
			)
		);
		final ScenarioConfig scenarioConfig = config.getTestConfig().getScenarioConfig();
		assertThat(scenarioConfig, notNullValue());
		assertThat(scenarioConfig.getFile(), nullValue("run.file"));
		final StorageConfig storageConfig = config.getStorageConfig();
		assertThat(storageConfig, notNullValue());
		final AuthConfig authConfig = storageConfig.getAuthConfig();
		assertThat(authConfig, notNullValue());
		assertThat(authConfig.getUid(), nullValue("storage.auth.uid"));
		assertThat(authConfig.getSecret(), nullValue("storage.auth.secret"));
		assertThat(authConfig.getToken(), nullValue("storage.auth.token"));
		final HttpConfig httpConfig = storageConfig.getNetConfig().getHttpConfig();
		assertThat(httpConfig, notNullValue());
		assertThat(httpConfig.getFsAccess(), equalTo(false, "storage.net.http.fsAccess"));
		final Map<String, String> headers = httpConfig.getHeadersConfig();
		assertThat(headers, notNullValue());
		assertThat(headers.containsKey(HttpConfig.KEY_HEADER_CONNECTION),
			equalTo(true, "storage.net.http.headers[Connection]"));
		assertThat(headers.get(HttpConfig.KEY_HEADER_CONNECTION),
			equalTo("Keep-Alive", "storage.net.http.headers[Connection]"));
		assertThat(headers.containsKey(HttpConfig.KEY_HEADER_USER_AGENT),
			equalTo(true, "storage.net.http.headers[User-Agent]"));
		assertThat(headers.get(HttpConfig.KEY_HEADER_USER_AGENT),
			equalTo("mongoose/3.5.0", "storage.net.http.headers[User-Agent]"));
		assertThat(httpConfig.getNamespace(), nullValue("storage.net.http.namespace"));
		assertThat(httpConfig.getVersioning(), equalTo(false, "storage.net.http.versioning"));
		assertThat(
			storageConfig.getNetConfig().getNodeConfig().getAddrs().get(0),
			equalTo("127.0.0.1", "storage.net.node.addrs")
		);
		final DriverConfig driverConfig = storageConfig.getDriverConfig();
		assertThat(storageConfig.getNetConfig().getSsl(), equalTo(false, "storage.driver.remote"));
		assertThat(
			driverConfig.getAddrs().get(0),
			equalTo("127.0.0.1", "storage.driver.addrs")
		);
		assertThat(storageConfig.getNetConfig().getNodeConfig().getPort(), equalTo(9020, "storage.port"));
		assertThat(storageConfig.getNetConfig().getSsl(), equalTo(false, "storage.net.ssl."));
		final MockConfig mockConfig = storageConfig.getMockConfig();
		assertThat(mockConfig, notNullValue());
		assertThat(mockConfig.getCapacity(), equalTo(1_000_000, "storage.mock.capacity"));
		assertThat(mockConfig.getNode(), equalTo(false, "storage.mock.node"));
		final MockConfig.ContainerConfig containerConfig = mockConfig
			.getContainerConfig();
		assertThat(containerConfig, notNullValue());
		assertThat(
			containerConfig.getCapacity(), equalTo(1_000_000, "storage.mock.container.capacity"));
		assertThat(
			containerConfig.getCountLimit(),
			equalTo(1_000_000, "storage.mock.container.countLimit")
		);
	}
	
}
