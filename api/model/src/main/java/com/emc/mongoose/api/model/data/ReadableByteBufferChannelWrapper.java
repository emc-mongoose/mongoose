package com.emc.mongoose.api.model.data;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 Created by andrey on 10.09.17.
 */
public interface ReadableByteBufferChannelWrapper<B extends ByteBuffer>
extends ReadableByteChannel {

	B getByteBuffer();
}
