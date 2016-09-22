package com.emc.mongoose.model.impl.item;

import com.emc.mongoose.model.api.data.ContentSource;
import com.emc.mongoose.model.api.item.DataItem;
import com.emc.mongoose.model.impl.data.DataCorruptionException;
import com.emc.mongoose.model.impl.data.DataSizeException;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer. Not thread safe.
 */
public class BasicDataItem
extends BasicItem
implements DataItem {
	//
	private static final String
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"";
	protected static final String
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s";
	//
	private transient ContentSource contentSrc;
	private transient int ringBuffSize;
	//
	protected int layerNum = 0;
	//
	protected long offset = 0;
	protected long position = 0;
	protected long size = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicDataItem() {
		super();
	}
	//
	public BasicDataItem(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
		this.ringBuffSize = contentSrc.getSize();
		//setRingBuffer(contentSrc.getLayer(0).asReadOnlyBuffer());
	}
	//
	public BasicDataItem(final String value, final ContentSource contentSrc) {
		this(contentSrc);
		fromString(value);
	}
	//
	public BasicDataItem(
		final long offset, final long size, final ContentSource contentSrc
	) {
		this(SLASH, Long.toString(offset, Character.MAX_RADIX), offset, size, 0, contentSrc);
	}
	//
	public BasicDataItem(
		final String name, final long offset, final long size, final ContentSource contentSrc
	) {
		this(SLASH, name, offset, size, 0, contentSrc);
	}
	//
	public BasicDataItem(
		final String path, final String name, final long offset, final long size,
		final ContentSource contentSrc
	) {
		this(path, name, offset, size, 0, contentSrc);
	}
	//
	public BasicDataItem(
		final long offset, final long size, final int layerNum, final ContentSource contentSrc
	) {
		this(contentSrc);
		this.layerNum = layerNum;
		this.offset = offset;
		this.size = size;
	}
	//
	public BasicDataItem(
		final String name, final long offset, final long size, final int layerNum,
		final ContentSource contentSrc
	) {
		super(SLASH, name);
		this.contentSrc = contentSrc;
		this.ringBuffSize = contentSrc.getSize();
		this.layerNum = layerNum;
		this.offset = offset;
		this.size = size;
	}
	//
	public BasicDataItem(
		final String path, final String name, final long offset, final long size,
		final int layerNum, final ContentSource contentSrc
	) {
		super(path, name);
		this.contentSrc = contentSrc;
		this.ringBuffSize = contentSrc.getSize();
		this.layerNum = layerNum;
		this.offset = offset;
		this.size = size;
	}
	//
	public BasicDataItem(
		final BasicDataItem baseDataItem, final long internalOffset, final long size,
		final boolean nextLayer
	) {
		this.contentSrc = baseDataItem.contentSrc;
		this.ringBuffSize = baseDataItem.ringBuffSize;
		this.offset = baseDataItem.offset + internalOffset;
		this.size = size;
		this.layerNum = nextLayer ? baseDataItem.layerNum : baseDataItem.layerNum;
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	protected void fromString(final String value) {
		final String tokens[] = value.split(",", 3);
		if(tokens.length == 3) {
			super.fromString(tokens[0]);
			try {
				setOffset(Long.parseLong(tokens[1], 0x10));
			} catch(final NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_OFFSET, tokens[1]));
			}
			try {
				truncate(Long.parseLong(tokens[2], 10));
			} catch(final NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_SIZE, tokens[2]));
			}
		} else {
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, value));
		}
	}
	//
	private final static ThreadLocal<StringBuilder> THR_LOCAL_STR_BUILDER = new ThreadLocal<>();
	@Override
	public String toString() {
		StringBuilder strBuilder = THR_LOCAL_STR_BUILDER.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THR_LOCAL_STR_BUILDER.set(strBuilder);
		} else {
			strBuilder.setLength(0); // reset
		}
		return strBuilder
			.append(super.toString()).append(",")
			.append(Long.toString(offset, 0x10)).append(",")
			.append(size).toString();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final ContentSource getContentSrc() {
		return contentSrc;
	}
	//
	@Override
	public final void setContentSrc(final ContentSource contentSrc) {
		this.contentSrc = contentSrc;
		this.ringBuffSize = contentSrc.getSize();
	}
	//
	@Override
	public void reset() {
		layerNum = 0;
		position = 0;
	}
	//
	@Override
	public final long getOffset() {
		return offset;
	}
	//
	@Override
	public final void setOffset(final long offset) {
		this.offset = offset < 0 ? Long.MAX_VALUE + offset + 1 : offset;
		reset();
	}
	//
	public long position() {
		return position;
	}
	//
	@Override
	public final BasicDataItem position(final long position) {
		this.position = position;
		return this;
	}
	//
	@Override
	public long size() {
		return size;
	}
	//
	@Override
	public BasicDataItem truncate(final long size) {
		this.size = size;
		return this;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ByteChannels implementation
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final void close() {
	}
	//
	@Override
	public final boolean isOpen() {
		return true;
	}
	//
	@Override
	public final int read(final ByteBuffer dst) {
		final int n;
		final ByteBuffer ringBuff = contentSrc.getLayer(layerNum).asReadOnlyBuffer();
		ringBuff.position((int) (offset + position) % ringBuffSize);
		// bytes count to transfer
		n = Math.min(dst.remaining(), ringBuff.remaining());
		ringBuff.limit(ringBuff.position() + n);
		// do the transfer
		dst.put(ringBuff);
		position += n;
		return n;
	}
	//
	@Override
	public final int write(final ByteBuffer src)
	throws DataCorruptionException, DataSizeException {
		if(src == null) {
			return 0;
		}
		int m;
		final ByteBuffer ringBuff = contentSrc.getLayer(layerNum).asReadOnlyBuffer();
		ringBuff.position((int) (offset + position) % ringBuffSize);
		final int n = Math.min(src.remaining(), ringBuff.remaining());
		if(n > 0) {
			byte bs, bi;
			for(m = 0; m < n; m ++) {
				bs = ringBuff.get();
				bi = src.get();
				if(bs != bi) {
					throw new DataCorruptionException(m, bs, bi);
				}
			}
			position += n;
		} else {
			return n;
		}
		return m;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException {
		final ByteBuffer ringBuff = contentSrc.getLayer(layerNum).asReadOnlyBuffer();
		ringBuff.position((int) ((offset + position) % ringBuffSize));
		int n = (int) Math.min(maxCount, ringBuff.remaining());
		ringBuff.limit(ringBuff.position() + n);
		n = chanDst.write(ringBuff);
		position += n;
		return n;
	}
	//
	@Override
	public final int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataCorruptionException, IOException {
		int n;
		final ByteBuffer ringBuff = contentSrc.getLayer(layerNum).asReadOnlyBuffer();
		ringBuff.position((int) (offset + position) % ringBuffSize);
		n = ringBuff.remaining();
		if(buff.limit() > n) {
			buff.limit(n);
		}
		//
		n = chanSrc.read(buff);
		//
		if(n > 0) {
			byte bs, bi;
			buff.flip();
			for(int m = 0; m < n; m++) {
				bs = ringBuff.get();
				bi = buff.get();
				if(bs != bi) {
					throw new DataCorruptionException(m, bs, bi);
				}
			}
			position += n;
		}
		return n;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(final Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof BasicDataItem)) {
			return false;
		}
		final BasicDataItem other = (BasicDataItem) o;
		return (size == other.size) && (offset == other.offset);
	}
	//
	@Override
	public int hashCode() {
		return (int) (offset ^ size);
	}
	
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		super.writeExternal(out);
		out.writeInt(layerNum);
		out.writeLong(offset);
		out.writeLong(position);
		out.writeLong(size);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		layerNum = in.readInt();
		offset = in.readLong();
		position = in.readLong();
		size = in.readLong();
	}
}
