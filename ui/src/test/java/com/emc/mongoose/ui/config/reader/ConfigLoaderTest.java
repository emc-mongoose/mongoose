package com.emc.mongoose.ui.config.reader;

import com.emc.mongoose.ui.config.Config;
import com.emc.mongoose.model.util.SizeInBytes;
import com.emc.mongoose.model.util.TimeUtil;
import com.emc.mongoose.ui.config.reader.jackson.ConfigLoader;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static com.emc.mongoose.ui.config.matcher.ConfigMatcher.equalTo;
import static com.emc.mongoose.ui.config.matcher.ConfigNullMatcher.notNullValue;
import static com.emc.mongoose.ui.config.matcher.ConfigNullMatcher.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 Created by kurila on 14.07.16.
 */
public class ConfigLoaderTest {

	@SuppressWarnings("ConstantConditions")
	@Test
	public void shouldParseWithoutFireballsThrowing()
	throws IOException {
		final Config config = ConfigLoader.loadDefaultConfig();
		assertThat(config, notNullValue());
		assertThat(config.getName(), equalTo("mongoose", "name"));
		assertThat(config.getVersion(), equalTo("3.0.0-SNAPSHOT", "version"));
		final Config.IoConfig ioConfig = config.getIoConfig();
		assertThat(ioConfig, notNullValue());
		final Config.IoConfig.BufferConfig bufferConfig = ioConfig.getBufferConfig();
		assertThat(bufferConfig, notNullValue());
		assertThat(bufferConfig.getSize(), equalTo(new SizeInBytes("4KB-1MB"), "io.buffer.size"));
		final Config.SocketConfig socketConfig = config.getSocketConfig();
		assertThat(socketConfig, notNullValue());
		assertThat(socketConfig.getTimeoutMilliSec(), equalTo(1_000_000, "socket.timeoutMilliSec"));
		assertThat(socketConfig.getReuseAddr(), equalTo(true, "socket.reuseAddr"));
		assertThat(socketConfig.getKeepAlive(), equalTo(true, "socket.keepAlive"));
		assertThat(socketConfig.getTcpNoDelay(), equalTo(true, "socket.tcpNoDelay"));
		assertThat(socketConfig.getLinger(), equalTo(0, "socket.linger"));
		assertThat(socketConfig.getBindBackLogSize(), equalTo(0, "socket.bindBacklogSize"));
		assertThat(socketConfig.getInterestOpQueued(), equalTo(false, "socket.interestOpQueued"));
		assertThat(socketConfig.getSelectInterval(), equalTo(100, "socket.selectInterval"));
		final Config.ItemConfig itemConfig = config.getItemConfig();
		assertThat(itemConfig, notNullValue());
		assertThat(itemConfig.getType(), equalTo("data", "item.type"));
		final Config.ItemConfig.DataConfig dataConfig = itemConfig.getDataConfig();
		assertThat(dataConfig, notNullValue());
		final Config.ItemConfig.DataConfig.ContentConfig contentConfig =
			dataConfig.getContentConfig();
		assertThat(contentConfig, notNullValue());
		assertThat(contentConfig.getFile(), nullValue("item.data.content.file"));
		assertThat(contentConfig.getSeed(), equalTo("7a42d9c483244167", "item.data.content.seed"));
		assertThat(contentConfig.getRingSize(), equalTo(new SizeInBytes("4MB"), "item.data.content.ringSize"));
		assertThat(dataConfig.getRanges().getRandomCount(), equalTo(0, "item.data.ranges"));
		assertThat(dataConfig.getSize(), equalTo(new SizeInBytes("1MB"), "item.data.size"));
		assertThat(dataConfig.getVerify(), equalTo(true, "item.data.verify"));
		final Config.ItemConfig.InputConfig inputConfig = itemConfig.getInputConfig();
		assertThat(inputConfig, notNullValue());
		assertThat(inputConfig.getContainer(), nullValue("item.input.container"));
		assertThat(inputConfig.getFile(), nullValue("item.input.file"));
		final Config.ItemConfig.OutputConfig outputConfig= itemConfig.getOutputConfig();
		assertThat(outputConfig, notNullValue());
		assertThat(outputConfig.getContainer(), nullValue("item.output.container"));
		assertThat(outputConfig.getFile(), nullValue("item.output.file"));
		final Config.ItemConfig.NamingConfig namingConfig = itemConfig.getNamingConfig();
		assertThat(namingConfig, notNullValue());
		assertThat(namingConfig.getType(), equalTo("random", "item.naming.type"));
		assertThat(namingConfig.getPrefix(), nullValue("item.naming.prefix"));
		assertThat(namingConfig.getRadix(), equalTo(36, "item.naming.radix"));
		assertThat(namingConfig.getOffset(), equalTo(0L, "item.naming.offset"));
		assertThat(namingConfig.getLength(), equalTo(13, "item.naming.length"));
		final Config.LoadConfig loadConfig = config.getLoadConfig();
		assertThat(loadConfig, notNullValue());
		assertThat(loadConfig.getCircular(), equalTo(false, "load.circular"));
		assertThat(loadConfig.getType(), equalTo("create", "load.type"));
		assertThat(loadConfig.getConcurrency(), equalTo(1, "load.concurrency"));
		final Config.LoadConfig.LimitConfig limitConfig = loadConfig.getLimitConfig();
		assertThat(limitConfig, notNullValue());
		assertThat(limitConfig.getCount(), equalTo(0L, "load.limit.count"));
		assertThat(limitConfig.getRate(), equalTo(0.0, "load.limit.rate"));
		assertThat(limitConfig.getSize(), equalTo(new SizeInBytes(0), "load.limit.size"));
		final String timeTestValue = "0s";
		assertThat(limitConfig.getTime(), equalTo(TimeUtil.getTimeUnit(timeTestValue).toSeconds(TimeUtil.getTimeValue(timeTestValue)), "load.limit.time"));
		final Config.LoadConfig.MetricsConfig metricsConfig = loadConfig.getMetricsConfig();
		assertThat(metricsConfig, notNullValue());
		assertThat(metricsConfig.getIntermediate(), equalTo(false, "load.metrics.intermediate"));
		final String periodTestValue = "10s";
		assertThat(metricsConfig.getPeriod(), equalTo(TimeUtil.getTimeUnit(periodTestValue).toSeconds(TimeUtil.getTimeValue(periodTestValue)), "load.metrics.period"));
		assertThat(metricsConfig.getPrecondition(), equalTo(false, "load.metrics.precondition"));
		final Config.RunConfig runConfig = config.getRunConfig();
		assertThat(runConfig, notNullValue());
		assertThat(runConfig.getId(), nullValue("run.id"));
		assertThat(runConfig.getFile(), nullValue("run.file"));
		final Config.StorageConfig storageConfig = config.getStorageConfig();
		assertThat(storageConfig, notNullValue());
		assertThat(storageConfig.getAddrs().get(0), equalTo("127.0.0.1", "storage.address"));
		final Config.StorageConfig.AuthConfig authConfig = storageConfig.getAuthConfig();
		assertThat(authConfig, notNullValue());
		assertThat(authConfig.getId(), nullValue("storage.auth.id"));
		assertThat(authConfig.getSecret(), nullValue("storage.auth.secret"));
		assertThat(authConfig.getToken(), nullValue("storage.auth.token"));
		final Config.StorageConfig.HttpConfig httpConfig = storageConfig.getHttpConfig();
		assertThat(httpConfig, notNullValue());
		assertThat(httpConfig.getApi(), equalTo("S3", "storage.http.api"));
		assertThat(httpConfig.getFsAccess(), equalTo(false, "storage.http.fsAccess"));
		final Map<String, String> headers = httpConfig.getHeaders();
		assertThat(headers, notNullValue());
		assertThat(headers.containsKey(Config.StorageConfig.HttpConfig.KEY_HEADER_CONNECTION),
			equalTo(true, "storage.http.headers[Connection]"));
		assertThat(headers.get(Config.StorageConfig.HttpConfig.KEY_HEADER_CONNECTION),
			equalTo("keep-alive", "storage.http.headers[Connection]"));
		assertThat(headers.containsKey(Config.StorageConfig.HttpConfig.KEY_HEADER_USER_AGENT),
			equalTo(true, "storage.http.headers[User-Agent]"));
		assertThat(headers.get(Config.StorageConfig.HttpConfig.KEY_HEADER_USER_AGENT),
			equalTo("mongoose/3.0.0-SNAPSHOT", "storage.http.headers[User-Agent]"));
		assertThat(httpConfig.getNamespace(), nullValue("storage.http.namespace"));
		assertThat(httpConfig.getVersioning(), equalTo(false, "storage.http.versioning"));
		assertThat(storageConfig.getPort(), equalTo(9020, "storage.port"));
		assertThat(storageConfig.getSsl(), equalTo(false, "storage.ssl"));
		assertThat(storageConfig.getType(), equalTo("http", "storage.type"));
		final Config.StorageConfig.MockConfig mockConfig = storageConfig.getMockConfig();
		assertThat(mockConfig, notNullValue());
		assertThat(mockConfig.getHeadCount() == 1 || mockConfig.getHeadCount() == 5, equalTo(true, "storage.mock.headCount"));
		assertThat(mockConfig.getCapacity(), equalTo(1_000_000, "storage.mock.capacity"));
		final Config.StorageConfig.MockConfig.ContainerConfig containerConfig =
			mockConfig.getContainerConfig();
		assertThat(containerConfig, notNullValue());
		assertThat(containerConfig.getCapacity(), equalTo(1_000_000, "storage.mock.container.capacity"));
		assertThat(containerConfig.getCountLimit(), equalTo(1_000_000, "storage.mock.container.countLimit"));
	}
	
}
