package com.emc.mongoose.api.model.io;

import com.emc.mongoose.api.model.data.DataCorruptionException;
import com.emc.mongoose.api.model.item.DataItem;

import com.github.akurilov.commons.io.ByteCountOutputStream;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class DataItemOutputStream
extends ByteCountOutputStream {

	private final DataItem dataItem;

	private ByteBuffer buffWrapper = null;
	private byte[] buff = null;

	public DataItemOutputStream(final DataItem dataItem) {
		this.dataItem = dataItem;
	}

	/**
	 @throws IOException does not throw actually
	 */
	@Override
	public final void write(final int b)
	throws DataCorruptionException, IOException {
		buffWrapper = ByteBuffer.allocate(1);
		dataItem.read(buffWrapper);
		buffWrapper.rewind();
		dataItem.verify(buffWrapper);
		byteCount.increment();
	}

	/**
	 @throws IOException does not throw actually
	 */
	@Override
	public final void write(final byte buff[], final int offset, final int length)
	throws DataCorruptionException, IOException {
		if(buff != this.buff) {
			buffWrapper = ByteBuffer.wrap(buff);
		}
		buffWrapper.position(offset).limit(offset + length);
		dataItem.verify(buffWrapper);
		byteCount.add(length);
	}

	/**
	 @throws IOException does not throw actually
	 */
	@Override
	public final void write(final byte buff[])
	throws DataCorruptionException, IOException {
		if(buff != this.buff) {
			buffWrapper = ByteBuffer.wrap(buff);
		}
		dataItem.verify(buffWrapper);
		byteCount.add(buff.length);
	}

	@Override
	public final void close()
	throws IOException {
		buffWrapper = null;
		buff = null;
	}
}
