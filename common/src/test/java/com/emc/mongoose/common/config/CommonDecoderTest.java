package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.CommonConfig;
import com.emc.mongoose.common.config.CommonDecoder;
import com.emc.mongoose.common.config.reader.ConfigReader;
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
		final CommonConfig.Network.Socket socket = commonConfig.network().socket();
		assertEquals(parameterErrorMessage("name"),
			commonConfig.getName(), "mongoose");
		assertEquals(parameterErrorMessage("network.socket.timeoutMilliSec"),
			socket.getTimeoutInMilliseconds(), 1_000_000);
		assertEquals(parameterErrorMessage("network.socket.reuseAddr"),
			socket.getReusableAddress(), true);
		assertEquals(parameterErrorMessage("network.socket.keepAlive"),
			socket.getKeepAlive(), true);
		assertEquals(parameterErrorMessage("network.socket.tcpNoDelay"),
			socket.getTcpNoDelay(), true);
		assertEquals(parameterErrorMessage("network.socket.linger"),
			socket.getLinger(), 0);
		assertEquals(parameterErrorMessage("network.socket.bindBacklogSize"),
			socket.getBindBackLogSize(), 0);
		assertEquals(parameterErrorMessage("network.socket.interestOpQueued"),
			socket.getInterestOpQueued(), false);
		assertEquals(parameterErrorMessage("network.socket.selectInterval"),
			socket.getSelectInterval(), 100);
	}

}
