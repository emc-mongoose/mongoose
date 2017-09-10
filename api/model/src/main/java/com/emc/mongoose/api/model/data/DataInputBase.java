package com.emc.mongoose.api.model.data;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

/**
 Created by andrey on 24.07.17.
 */
public abstract class DataInputBase
implements DataInput {

	protected ReadableByteBufferChannelWrapper<MappedByteBuffer> inputBuffChannel;

	protected DataInputBase() {
		inputBuffChannel = null;
	}

	protected DataInputBase(final MappedByteBuffer inputBuff) {
		this.inputBuffChannel = new ReadableMappedByteBufferChannelWrapper(inputBuff);
		inputBuff.clear();
	}

	protected DataInputBase(final DataInputBase other) {
		this.inputBuffChannel = other.inputBuffChannel;
	}

	@Override
	public final int getSize() {
		// NPE protection is necessary for the storage driver service
		return inputBuffChannel == null ? 0 : inputBuffChannel.getByteBuffer().capacity();
	}

	@Override
	public abstract ReadableByteBufferChannelWrapper<MappedByteBuffer> getLayer(
		final int layerIndex
	);

	@Override
	public void close()
	throws IOException {
		if(inputBuffChannel != null) {
			inputBuffChannel.close();
		}
		inputBuffChannel = null;
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		// write buffer capacity and data
		final MappedByteBuffer inputBuffW = inputBuffChannel.getByteBuffer();
		final byte buff[] = new byte[inputBuffW.capacity()];
		inputBuffW.clear(); // reset the position
		inputBuffW.get(buff);
		out.writeInt(buff.length);
		out.write(buff);
	}

	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		// read buffer data and wrap with ByteBuffer
		final int size = in.readInt();
		final byte buff[] = new byte[size];
		for(int i, j = 0; j < size;) {
			i = in.read(buff, j, size - j);
			if(i == -1) {
				break;
			} else {
				j += i;
			}
		}
		inputBuffChannel = new ReadableMappedByteBufferChannelWrapper(
			(MappedByteBuffer) ByteBuffer.allocateDirect(size).put(buff)
		);
	}
}
