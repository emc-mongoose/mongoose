package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.reader.ConfigReader;
import com.emc.mongoose.common.util.SizeInBytes;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
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

	@SuppressWarnings("ConstantConditions")
	@Test
	public void shouldCreateConfig() throws Exception {
		final CommonConfig commonConfig = ConfigReader.loadConfig(new CommonDecoder());
		assertThat(commonConfig, is(notNullValue()));
		assertThat(commonConfig.getName(), is(equalTo("mongoose")));
		assertThat(commonConfig.getVersion(), is(equalTo("3.0.0-SNAPSHOT")));
		final CommonConfig.IoConfig ioConfig = commonConfig.getIoConfig();
		assertThat(ioConfig, is(notNullValue()));
		final CommonConfig.IoConfig.BufferConfig bufferConfig = ioConfig.getBufferConfig();
		assertThat(bufferConfig, is(notNullValue()));
		assertThat(bufferConfig.getSize(), is(equalTo(new SizeInBytes("4KB-1MB"))));
		final CommonConfig.SocketConfig socketConfig = commonConfig.getSocketConfig();
		assertThat(socketConfig, is(notNullValue()));
	}

}
