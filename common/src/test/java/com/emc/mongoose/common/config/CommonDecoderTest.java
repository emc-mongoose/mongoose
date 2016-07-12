package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.decoder.Decoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 Created on 11.07.16.
 */
public class CommonDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final Decoder<CommonConfig> commonDecoder = new CommonDecoder();
		final CommonConfig commonConfig = ConfigReader.loadConfig(commonDecoder);
		assertNotNull(commonConfig);
		final CommonConfig.NetworkConfig.SocketConfig socketConfig = commonConfig.getNetworkConfig().getSocketConfig();
		assertEquals(parameterErrorMessage("name"),
			commonConfig.getName(), "mongoose");
		assertEquals(parameterErrorMessage("network.socketConfig.timeoutMilliSec"),
			socketConfig.getTimeoutInMilliseconds(), 1_000_000);
		assertEquals(parameterErrorMessage("network.socketConfig.reuseAddr"),
			socketConfig.getReusableAddress(), true);
		assertEquals(parameterErrorMessage("network.socketConfig.keepAlive"),
			socketConfig.getKeepAlive(), true);
		assertEquals(parameterErrorMessage("network.socketConfig.tcpNoDelay"),
			socketConfig.getTcpNoDelay(), true);
		assertEquals(parameterErrorMessage("network.socketConfig.linger"),
			socketConfig.getLinger(), 0);
		assertEquals(parameterErrorMessage("network.socketConfig.bindBacklogSize"),
			socketConfig.getBindBackLogSize(), 0);
		assertEquals(parameterErrorMessage("network.socketConfig.interestOpQueued"),
			socketConfig.getInterestOpQueued(), false);
		assertEquals(parameterErrorMessage("network.socketConfig.selectInterval"),
			socketConfig.getSelectInterval(), 100);
		final CommonConfig.StorageConfig storage = commonConfig.getStorageConfig();
		assertEquals(parameterErrorMessage("storage.addrs"),
			storage.getAddresses().get(0), "127.0.0.1");
		final CommonConfig.StorageConfig.AuthConfig auth = storage.getAuthConfig();
		assertNull(parameterErrorMessage("storage.auth.id"), auth.getId());
		assertNull(parameterErrorMessage("storage.auth.secret"), auth.getSecret());
		assertNull(parameterErrorMessage("storage.auth.token"), auth.getToken());
		final CommonConfig.StorageConfig.HttpConfig http = storage.getHttpConfig();
		assertEquals(parameterErrorMessage("storage.http.api"), http.getApi(), "S3");
		assertEquals(parameterErrorMessage("storage.http.fsAccess"), http.getFsAccess(), false);
		final Map<String, String> headers = http.getHeaders();
		assertEquals(parameterErrorMessage("storage.http.headers[\"Connection\"]"), headers.get(
			CommonConfig.StorageConfig.HttpConfig.KEY_HEADER_CONNECTION), "keep-alive");
		assertEquals(parameterErrorMessage("storage.http.headers[\"User-Agent\"]"), headers.get(
			CommonConfig.StorageConfig.HttpConfig.KEY_HEADER_USER_AGENT), "mongoose/3.0.0");
		assertNull("storage.http.namespace", http.getNamespace());
		assertEquals("storage.http.versioning", http.getVersioning(), false);
		assertEquals("storage.port", storage.getPort(), 9020);
		assertEquals("storage.type", storage.getType(), "http");
		final CommonConfig.ItemConfig itemConfig = commonConfig.getItemConfig();
		assertEquals(parameterErrorMessage("getItemConfig.type"), itemConfig.getType(), "data");
		final CommonConfig.ItemConfig.DataConfig dataConfig = itemConfig.getDataConfig();
		final CommonConfig.ItemConfig.DataConfig.ContentConfig contentConfig = dataConfig.getContentConfig();
		assertNull(parameterErrorMessage("item.data.content.file"), contentConfig.getFile());
		assertEquals(parameterErrorMessage("item.data.content.seed"), contentConfig.getSeed(), "7a42d9c483244167");
		assertEquals(parameterErrorMessage("item.data.content.ringSize"), contentConfig.getRingSize(), "4MB");
		assertEquals(parameterErrorMessage("item.data.ranges"), dataConfig.getRanges(), 0);
		assertEquals(parameterErrorMessage("item.data.size"), dataConfig.getSize(), "1MB");
		assertEquals(parameterErrorMessage("item.data.verify"), dataConfig.getVerify(), true);
		final CommonConfig.ItemConfig.DestinationConfig destinationConfig = itemConfig.getDestinationConfig();
		assertNull(parameterErrorMessage("item.dst.container"), destinationConfig.getContainer());
		assertNull(parameterErrorMessage("item.dst.file"), destinationConfig.getFile());
		final CommonConfig.ItemConfig.SourceConfig sourceConfig = itemConfig.getSourceConfig();
		assertNull(parameterErrorMessage("item.src.container"), sourceConfig.getContainer());
		assertNull(parameterErrorMessage("item.src.file"), sourceConfig.getFile());
		assertEquals(parameterErrorMessage("item.src.batchSize"), sourceConfig.getBatchSize(), 4096);
		final CommonConfig.ItemConfig.NamingConfig namingConfig = itemConfig.getNamingConfig();
		assertEquals(parameterErrorMessage("naming.type"), namingConfig.getType(), "random");
		assertNull(parameterErrorMessage("naming.prefix"), namingConfig.getPrefix());
		assertEquals(parameterErrorMessage("naming.radix"), namingConfig.getRadix(), 36);
		assertEquals(parameterErrorMessage("naming.offset"), namingConfig.getOffset(), 0);
		assertEquals(parameterErrorMessage("naming.length"), namingConfig.getLength(), 13);
	}

}
