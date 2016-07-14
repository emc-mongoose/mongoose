package com.emc.mongoose.common.item;

import com.emc.mongoose.common.data.ContentSource;
import com.emc.mongoose.common.data.DataCorruptionException;
import com.emc.mongoose.common.data.DataSizeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
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
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"";
	protected final static String
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s";
	//
	private ByteBuffer ringBuff;
	private int ringBuffSize;
	protected long offset = 0, size = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicDataItem(final String value) {
		fromString(value);
	}
	//
	public BasicDataItem(final ContentSource contentSrc) {
		setRingBuffer(contentSrc.getLayer(0).asReadOnlyBuffer());
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
		setRingBuffer(contentSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.size = size;
	}
	//
	public BasicDataItem(
		final String name, final long offset, final long size, final int layerNum,
		final ContentSource contentSrc
	) {
		super(SLASH, name);
		setRingBuffer(contentSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.size = size;
	}
	//
	public BasicDataItem(
		final String path, final String name, final long offset, final long size,
		final int layerNum, final ContentSource contentSrc
	) {
		super(path, name);
		setRingBuffer(contentSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.size = size;
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
				setSize(Long.parseLong(tokens[2], 10));
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
	//
	private void setRingBuffer(final ByteBuffer ringBuff) {
		synchronized(ringBuff) {
			this.ringBuff = ringBuff;
			ringBuffSize = ringBuff.capacity();
		}
	}
	//
	private void makeCircular() {
		final int currPos = ringBuff.position();
		if(currPos == ringBuffSize) {
			ringBuff.clear();
		} else if(currPos == ringBuff.limit()) {
			ringBuff.limit(ringBuffSize);
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void reset() {
		synchronized(ringBuff) {
			ringBuff.limit(ringBuffSize).position((int)(offset % ringBuffSize));
		}
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
	public final int getRelativeOffset() {
		return ringBuff.position();
	}
	//
	public final void setRelativeOffset(final long relOffset) {
		ringBuff.limit(ringBuffSize).position((int) ((offset + relOffset) % ringBuffSize));
	}
	//
	@Override
	public long getSize() {
		return size;
	}
	//
	@Override
	public void setSize(final long size) {
		this.size = size;
	}
	//
	@Override
	public final void setContentSource(final ContentSource dataSrc, final int overlayIndex) {
		setRingBuffer(dataSrc.getLayer(overlayIndex).asReadOnlyBuffer());
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
		synchronized(ringBuff) {
			makeCircular();
			// bytes count to transfer
			n = Math.min(dst.remaining(), ringBuff.remaining());
			ringBuff.limit(ringBuff.position() + n);
			// do the transfer
			dst.put(ringBuff);
		}
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
		synchronized(ringBuff) {
			makeCircular();
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
			} else {
				return n;
			}
		}
		return m;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException {
		synchronized(ringBuff) {
			makeCircular();
			int n = (int) Math.min(maxCount, ringBuff.remaining());
			ringBuff.limit(ringBuff.position() + n);
			return chanDst.write(ringBuff);
		}
	}
	//
	@Override
	public final int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataCorruptionException, IOException {
		int n;
		synchronized(ringBuff) {
			makeCircular();
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
			}
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
}
