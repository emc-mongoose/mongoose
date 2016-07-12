package com.emc.mongoose.common.config;

import com.emc.mongoose.common.config.decoder.DecodeException;
import com.emc.mongoose.common.config.decoder.Decoder;

import javax.json.JsonObject;

/**
 Created on 11.07.16.
 */
public class CommonDecoder implements Decoder<CommonConfig> {

	@Override
	public CommonConfig decode(final JsonObject commonJson)
	throws DecodeException {
		final JsonObject socketJson = commonJson.getJsonObject(CommonConfig.KEY_NETWORK)
 			.getJsonObject(CommonConfig.NetworkConfig.KEY_SOCKET);
		final CommonConfig.NetworkConfig.SocketConfig
			socketConfig = new CommonConfig.NetworkConfig.SocketConfig(
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_TIMEOUT_IN_MILLISECONDS),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_REUSABLE_ADDRESS),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_KEEP_ALIVE),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_TCP_NO_DELAY),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_LINGER),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_BIND_BACK_LOG_SIZE),
			socketJson.getBoolean(CommonConfig.NetworkConfig.SocketConfig.KEY_INTEREST_OP_QUEUED),
			socketJson.getInt(CommonConfig.NetworkConfig.SocketConfig.KEY_SELECT_INTERVAL)
		);
		return new CommonConfig(
			getString(commonJson, CommonConfig.KEY_NAME),
			new CommonConfig.NetworkConfig(socketConfig)
		);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
