package com.emc.mongoose.base.item.io;

import com.emc.mongoose.base.item.DataItem;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/** NOTE: the class is not thread safe! */
public final class DataItemInputStream extends InputStream {

	private final DataItem dataItem;
	private final long dataSize;

	private long doneSize = 0;
	private long markPos = 0;
	private ByteBuffer buffWrapper = null;
	private byte[] buff = null;

	public DataItemInputStream(final DataItem dataItem) throws IOException {
		this.dataItem = dataItem;
		this.dataSize = dataItem.size();
	}

	@Override
	public final int read() throws IOException {
		if (0 < dataSize - doneSize) {
			buffWrapper = ByteBuffer.allocate(1);
			dataItem.read(buffWrapper);
			doneSize++;
			return buffWrapper.get();
		} else {
			return -1;
		}
	}

	@Override
	public final int read(final byte[] buff, final int offset, final int length) throws IOException {
		if (0 < dataSize - doneSize) {
			if (buff != this.buff) {
				buffWrapper = ByteBuffer.wrap(buff);
			}
			buffWrapper.position(offset).limit(offset + length);
			final int n = dataItem.read(buffWrapper);
			doneSize += n;
			return n;
		} else {
			return -1;
		}
	}

	@Override
	public final int available() {
		return (int) Math.min(Integer.MAX_VALUE, dataSize - doneSize);
	}

	@Override
	public final boolean markSupported() {
		return true;
	}

	@Override
	public final void mark(final int readLimit) {
		try {
			this.markPos = dataItem.position();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final void reset() throws IOException {
		dataItem.position(markPos);
	}

	@Override
	public final void close() {
		buffWrapper = null;
		buff = null;
	}
}
