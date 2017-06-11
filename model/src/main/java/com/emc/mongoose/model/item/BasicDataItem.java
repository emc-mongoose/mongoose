package com.emc.mongoose.model.item;

import com.emc.mongoose.model.data.ContentSource;
import com.emc.mongoose.model.data.DataCorruptionException;
import com.emc.mongoose.model.data.DataSizeException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import static com.emc.mongoose.model.item.DataItem.getRangeOffset;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.BitSet;
import static java.lang.Math.min;

/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer. Not thread safe.
 Note: the {@link java.nio.channels.ReadableByteChannel#read(java.nio.ByteBuffer)}
 method implementation will not return 0 or -1 (endless)
 */
public class BasicDataItem
extends BasicItem
implements DataItem {
	//
	private static final String
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"",
		FMT_MSG_MASK = "Ranges mask is not correct hexadecimal value: %s",
		STR_EMPTY_MASK = "0";
	//
	private static final char LAYER_MASK_SEP = '/';
	//
	private volatile ContentSource contentSrc;
	private int ringBuffSize;
	//
	protected int layerNum = 0;
	//
	protected long offset = 0;
	protected long position = 0;
	protected long size = 0;
	//
	protected final BitSet modifiedRangesMask = new BitSet(Long.SIZE);
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicDataItem() {
		super();
	}
	//
	public BasicDataItem(final String value) {
		this(value, value.indexOf(','));
	}
	//
	private BasicDataItem(final String value, final int firstCommaPos) {

		super(value.substring(0, firstCommaPos));

		int prevCommaPos = firstCommaPos;
		int nextCommaPos = value.indexOf(',', prevCommaPos + 1);
		if(nextCommaPos < prevCommaPos) {
			throw new IllegalArgumentException("Invalid data item description: " + value);
		}
		final String offsetInfo = value.substring(prevCommaPos + 1, nextCommaPos);
		try {
			offset(Long.parseLong(offsetInfo, 0x10));
		} catch(final NumberFormatException e) {
			throw new IllegalArgumentException(String.format(FMT_MSG_OFFSET, offsetInfo));
		}

		prevCommaPos = nextCommaPos;
		nextCommaPos = value.indexOf(',', prevCommaPos + 1);
		if(nextCommaPos < prevCommaPos) {
			throw new IllegalArgumentException("Invalid data item description: " + value);
		}
		final String sizeInfo = value.substring(prevCommaPos + 1, nextCommaPos);
		try {
			truncate(Long.parseLong(sizeInfo, 10));
		} catch(final NumberFormatException e) {
			throw new IllegalArgumentException(String.format(FMT_MSG_SIZE, sizeInfo));
		}

		prevCommaPos = nextCommaPos;
		final String rangesInfo = value.substring(prevCommaPos + 1);
		final int sepPos = rangesInfo.indexOf(LAYER_MASK_SEP, 0);
		try {
			// extract hexadecimal layer number
			layerNum = Integer.parseInt(rangesInfo.substring(0, sepPos), 0x10);
			// extract hexadecimal mask, convert into bit set and add to the existing mask
			final String rangesMask = rangesInfo.substring(sepPos + 1, rangesInfo.length());
			final char rangesMaskChars[];
			if(rangesMask.length() == 0) {
				rangesMaskChars = ("00" + rangesMask).toCharArray();
			} else if(rangesMask.length() % 2 == 1) {
				rangesMaskChars = ("0" + rangesMask).toCharArray();
			} else {
				rangesMaskChars = rangesMask.toCharArray();
			}
			// method "or" to merge w/ the existing mask
			modifiedRangesMask.or(BitSet.valueOf(Hex.decodeHex(rangesMaskChars)));
		} catch(final DecoderException | NumberFormatException e) {
			throw new IllegalArgumentException(String.format(FMT_MSG_MASK, rangesInfo));
		}
	}
	//
	public BasicDataItem(final long offset, final long size) {
		this(Long.toString(offset, Character.MAX_RADIX), offset, size, 0);
	}
	//
	public BasicDataItem(final String name, final long offset, final long size) {
		this(name, offset, size, 0);
	}
	//
	public BasicDataItem(final long offset, final long size, final int layerNum) {
		this();
		this.layerNum = layerNum;
		this.offset = offset;
		this.size = size;
	}
	//
	public BasicDataItem(
		final String name, final long offset, final long size, final int layerNum
	) {
		super(name);
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
	private static final ThreadLocal<StringBuilder> STRB = new ThreadLocal<StringBuilder>() {
		@Override
		protected final StringBuilder initialValue() {
			return new StringBuilder();
		}
	};

	@Override
	public String toString() {
		final StringBuilder strb = STRB.get();
		strb.setLength(0); // reset
		return strb
			.append(super.toString()).append(',')
			.append(Long.toString(offset, 0x10)).append(',')
			.append(size).append(',')
			.append(Integer.toHexString(layerNum)).append('/')
			.append(
				modifiedRangesMask.isEmpty() ?
					STR_EMPTY_MASK : Hex.encodeHexString(modifiedRangesMask.toByteArray())
			).toString();
	}

	@Override
	public String toString(final String itemPath) {
		final StringBuilder strBuilder = STRB.get();
		strBuilder.setLength(0); // reset
		return strBuilder
			.append(super.toString(itemPath)).append(',')
			.append(Long.toString(offset, 0x10)).append(',')
			.append(size).append(',')
			.append(Integer.toHexString(layerNum)).append('/')
			.append(
				modifiedRangesMask.isEmpty() ?
					STR_EMPTY_MASK : Hex.encodeHexString(modifiedRangesMask.toByteArray())
			).toString();
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
		super.reset();
		position = 0;
	}
	//
	@Override
	public final int layer() {
		return layerNum;
	}
	//
	@Override
	public final void layer(final int layerNum) {
		this.layerNum = layerNum;
	}
	//
	@Override
	public final void size(final long size) {
		this.size = size;
	}
	//
	@Override
	public final long offset() {
		return offset;
	}
	//
	@Override
	public final void offset(final long offset) {
		this.offset = offset < 0 ? Long.MAX_VALUE + offset + 1 : offset;
		position = 0;
	}
	//
	@Override
	public BasicDataItem slice(final long from, final long partSize) {
		if(from < 0) {
			throw new IllegalArgumentException();
		}
		if(partSize < 1) {
			throw new IllegalArgumentException();
		}
		final BasicDataItem dataItemSlice = new BasicDataItem(
			name, offset + from, partSize, layerNum
		);
		if(contentSrc != null) {
			dataItemSlice.setContentSrc(contentSrc);
		}
		return dataItemSlice;
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
	//
	@Override
	public final long getRangeSize(final int i) {
		return min(getRangeOffset(i + 1), size) - getRangeOffset(i);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////
	// UPDATE //////////////////////////////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final boolean isUpdated() {
		return layerNum > 0 || !modifiedRangesMask.isEmpty();
	}
	
	@Override
	public final void commitUpdatedRanges(final BitSet[] updatingRangesMaskPair) {
		if(updatingRangesMaskPair[1].isEmpty()) {
			modifiedRangesMask.or(updatingRangesMaskPair[0]);
		} else {
			modifiedRangesMask.clear();
			modifiedRangesMask.or(updatingRangesMaskPair[1]);
			layerNum ++;
		}
	}
	
	@Override
	public final boolean isRangeUpdated(final int rangeIdx) {
		return modifiedRangesMask.get(rangeIdx);
	}
	
	@Override
	public final int getUpdatedRangesCount() {
		return modifiedRangesMask.cardinality();
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
		ringBuff.position((int) ((offset + position) % ringBuffSize));
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
		ringBuff.position((int) ((offset + position) % ringBuffSize));
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
	
	@Override
	public final int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataCorruptionException, IOException {
		int n;
		final ByteBuffer ringBuff = contentSrc.getLayer(layerNum).asReadOnlyBuffer();
		ringBuff.position((int) ((offset + position) % ringBuffSize));
		n = ringBuff.remaining();
		if(buff.limit() > n) {
			buff.limit(n);
		}
		//
		n = chanSrc.read(buff);
		//
		if(n > 0) {
			buff.flip();
			
			final int wordCount = n >>> 3;
			if(wordCount > 0) {
				long ws, wi;
				for(int k = 0; k < wordCount; k ++) {
					ws = ringBuff.getLong();
					wi = buff.getLong();
					if(ws != wi) {
						final int wordPos = k << 3;
						byte bs, bi;
						for(int i = 0; i < 8; i ++) {
							bs = (byte) (ws & 0xFF);
							ws >>= 8;
							bi = (byte) (wi & 0xFF);
							wi >>= 8;
							if(bs != bi) {
								throw new DataCorruptionException(wordPos + i, bs, bi);
							}
						}
					}
				}
			}

			final int tailByteCount = n & 7;
			if(tailByteCount > 0) {
				byte bs, bi;
				for(int m = 0; m < tailByteCount; m ++) {
					bs = ringBuff.get();
					bi = buff.get();
					if(bs != bi) {
						throw new DataCorruptionException(m, bs, bi);
					}
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
		final byte buff[] = modifiedRangesMask.toByteArray();
		out.writeInt(buff.length);
		out.write(buff);
	}
	
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		super.readExternal(in);
		layerNum = in.readInt();
		offset = in.readLong();
		position = in.readLong();
		size = in.readLong();
		final int len = in.readInt();
		final byte buff[] = new byte[len];
		in.readFully(buff);
		modifiedRangesMask.or(BitSet.valueOf(buff));
	}
}
