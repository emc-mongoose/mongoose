package com.emc.mongoose.config;

import com.emc.mongoose.config.reader.ConfigReader;
import org.junit.Test;

import javax.json.JsonObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 Created on 11.07.16.
 */
public class CommonDecoderTest {

	private static String parameterErrorMessage(final String content) {
		return "Wrong " + content + " parameter";
	}

	@Test
	public void shouldCreateConfig() throws Exception{
		final CommonDecoder commonDecoder = new CommonDecoder();
		final JsonObject defaults = ConfigReader.readJson("defaults.json");
		assertNotNull("The configuration file was read wrong", defaults);
		final CommonConfig commonConfig =
			commonDecoder.decode(defaults);
		final CommonConfig.NetworkConfig.SocketConfig socketConfig = commonConfig.getNetworkConfig().getSocketConfig();
		assertEquals(parameterErrorMessage("name"),
			commonConfig.getName(), "mongoose");
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.timeoutMilliSec"),
			socketConfig.getTimeoutInMilliseconds(), 1_000_000);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.reuseAddr"),
			socketConfig.getReusableAddress(), true);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.keepAlive"),
			socketConfig.getKeepAlive(), true);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.tcpNoDelay"),
			socketConfig.getTcpNoDelay(), true);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.linger"),
			socketConfig.getLinger(), 0);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.bindBacklogSize"),
			socketConfig.getBindBackLogSize(), 0);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.interestOpQueued"),
			socketConfig.getInterestOpQueued(), false);
		assertEquals(parameterErrorMessage("getNetworkConfig.socketConfig.selectInterval"),
			socketConfig.getSelectInterval(), 100);
	}

}
