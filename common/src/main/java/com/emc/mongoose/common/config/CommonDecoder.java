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
			.getJsonObject(CommonConfig.Network.KEY_SOCKET);
		final CommonConfig.Network.Socket socket = new CommonConfig.Network.Socket(
			socketJson.getInt(CommonConfig.Network.Socket.KEY_TIMEOUT_IN_MILLISECONDS),
			socketJson.getBoolean(CommonConfig.Network.Socket.KEY_REUSABLE_ADDRESS),
			socketJson.getBoolean(CommonConfig.Network.Socket.KEY_KEEP_ALIVE),
			socketJson.getBoolean(CommonConfig.Network.Socket.KEY_TCP_NO_DELAY),
			socketJson.getInt(CommonConfig.Network.Socket.KEY_LINGER),
			socketJson.getInt(CommonConfig.Network.Socket.KEY_BIND_BACK_LOG_SIZE),
			socketJson.getBoolean(CommonConfig.Network.Socket.KEY_INTEREST_OP_QUEUED),
			socketJson.getInt(CommonConfig.Network.Socket.KEY_SELECT_INTERVAL)
		);
		return new CommonConfig(
			commonJson.getString(CommonConfig.KEY_NAME),
			new CommonConfig.Network(socket)
		);
	}

	@Override
	public void init() {
	}

	@Override
	public void destroy() {
	}
}
