package com.emc.mongoose.storage.driver.net.base.data;

import static com.emc.mongoose.model.storage.StorageDriver.BUFF_SIZE_MAX;

import io.netty.handler.stream.ChunkedNioStream;

import java.io.IOException;
import java.nio.channels.SeekableByteChannel;

/**
 Created by andrey on 24.04.17.
 */
public final class SeekableByteChannelChunkedNioStream
extends ChunkedNioStream {

	private final long sizeToTransfer;

	public SeekableByteChannelChunkedNioStream(final SeekableByteChannel sbc)
	throws IOException {
		this(sbc, sbc.size());
	}

	private SeekableByteChannelChunkedNioStream(
		final SeekableByteChannel sbc, final long sizeToTransfer
	) {
		super(sbc, sizeToTransfer > BUFF_SIZE_MAX ? BUFF_SIZE_MAX : (int) sizeToTransfer);
		this.sizeToTransfer = sizeToTransfer;
	}

	@Override
	public long length() {
		return sizeToTransfer;
	}

	@Override
	public final boolean isEndOfInput() {
		return sizeToTransfer == transferredBytes();
	}
}
