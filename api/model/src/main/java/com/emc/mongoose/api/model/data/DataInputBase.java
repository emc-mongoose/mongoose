package com.emc.mongoose.api.model.data;

import com.github.akurilov.commons.system.DirectMemUtil;

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

	protected MappedByteBuffer inputBuff;

	protected DataInputBase() {
		inputBuff = null;
	}

	protected DataInputBase(final MappedByteBuffer inputBuff) {
		this.inputBuff = inputBuff;
		inputBuff.clear();
	}

	protected DataInputBase(final DataInputBase other) {
		this.inputBuff = other.inputBuff;
	}

	@Override
	public final int getSize() {
		// NPE protection is necessary for the storage driver service
		return inputBuff == null ? 0 : inputBuff.capacity();
	}

	@Override
	public abstract MappedByteBuffer getLayer(final int layerIndex);

	@Override
	public void close()
	throws IOException {
		if(inputBuff != null) {
			closeDirectBuffer(inputBuff);
			inputBuff = null;
		}
	}

	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		// write buffer capacity and data
		final byte buff[] = new byte[inputBuff.capacity()];
		inputBuff.clear(); // reset the position
		inputBuff.get(buff);
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
		inputBuff = (MappedByteBuffer) ByteBuffer.allocateDirect(size).put(buff);
	}
}
