package com.emc.mongoose.api.model.data;

import com.github.akurilov.commons.system.DirectMemUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 Created by andrey on 10.09.17.
 */
public class ReadableMappedByteBufferChannelWrapper
implements ReadableByteBufferChannelWrapper<MappedByteBuffer> {

	private final MappedByteBuffer wrappedBuff;

	public ReadableMappedByteBufferChannelWrapper(final MappedByteBuffer byteBuff) {
		wrappedBuff = byteBuff;
	}

	@Override
	public final int read(final ByteBuffer dstBuff)
	throws IOException {
		final int n = Math.min(wrappedBuff.remaining(), dstBuff.remaining());
		wrappedBuff.limit(wrappedBuff.position() + n);
		dstBuff.put(wrappedBuff);
		return n;
	}

	@Override
	public final boolean isOpen() {
		return true;
	}

	@Override
	public final MappedByteBuffer getByteBuffer() {
		return wrappedBuff;
	}

	@Override
	public final void close()
	throws IOException {
		DirectMemUtil.free(wrappedBuff);
	}
}
