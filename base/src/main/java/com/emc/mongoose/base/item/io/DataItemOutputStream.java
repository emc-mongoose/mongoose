package com.emc.mongoose.base.item.io;

import static com.github.akurilov.commons.lang.Exceptions.throwUnchecked;

import com.emc.mongoose.base.data.DataCorruptionException;
import com.emc.mongoose.base.item.DataItem;
import com.github.akurilov.commons.io.ByteCountOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public final class DataItemOutputStream extends ByteCountOutputStream {

	private final DataItem dataItem;

	private ByteBuffer buffWrapper = null;
	private byte[] buff = null;

	public DataItemOutputStream(final DataItem dataItem) {
		this.dataItem = dataItem;
	}

	/** @throws IOException does not throw actually */
	@Override
	public final void write(final int b) throws DataCorruptionException {
		buffWrapper = ByteBuffer.allocate(1);
		try {
			dataItem.read(buffWrapper);
			buffWrapper.rewind();
			dataItem.verify(buffWrapper);
			dataItem.position(dataItem.position() + 1);
		} catch (final IOException e) {
			throwUnchecked(e);
		}
		byteCount.increment();
	}

	/** @throws IOException does not throw actually */
	@Override
	@SuppressWarnings("ArrayEquality")
	public final void write(final byte buff[], final int offset, final int length)
					throws DataCorruptionException {
		if (buff != this.buff) {
			buffWrapper = ByteBuffer.wrap(buff);
		}
		buffWrapper.position(offset).limit(offset + length);
		dataItem.verify(buffWrapper);
		try {
			dataItem.position(dataItem.position() + length);
		} catch (final IOException e) {
			throwUnchecked(e);
		}
		byteCount.add(length);
	}

	/** @throws IOException does not throw actually */
	@Override
	@SuppressWarnings("ArrayEquality")
	public final void write(final byte buff[]) throws DataCorruptionException {
		if (buff != this.buff) {
			buffWrapper = ByteBuffer.wrap(buff);
		}
		dataItem.verify(buffWrapper);
		try {
			dataItem.position(dataItem.position() + buff.length);
		} catch (final IOException e) {
			throwUnchecked(e);
		}
		byteCount.add(buff.length);
	}

	@Override
	public final void close() throws IOException {
		buffWrapper = null;
		buff = null;
	}
}
