package com.emc.mongoose.core.impl.data;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.src.DataSource;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer.
 */
public class UniformData
extends ByteArrayInputStream
implements DataItem {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		FMT_META_INFO = "%x" + RunTimeConfig.LIST_SEP + "%d",
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"",
		FMT_MSG_FAIL_CHANGE_OFFSET = "Failed to change offset to \"%x\"",
		FMT_MSG_FAIL_SET_OFFSET = "Failed to set data ring offset: \"%s\"",
		FMT_MSG_STREAM_OUT_START = "Item \"{}\": stream out start",
		FMT_MSG_STREAM_OUT_FINISH = "Item \"{}\": stream out finish";
	protected final static String
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s",
		MSG_READ_RING_BLOCKED = "Reading from data ring blocked?";
	private static AtomicLong NEXT_OFFSET = new AtomicLong(
		Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
	);
	//
	protected long offset = 0;
	protected long size = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformData() {
		super(UniformDataSource.DEFAULT.getBytes(0));
	}
	//
	public UniformData(final String metaInfo) {
		this();
		fromString(metaInfo);
	}
	//
	public UniformData(final Long size) {
		this(size, UniformDataSource.DEFAULT);
	}
	//
	public UniformData(final Long size, final UniformDataSource dataSrc) {
		this(
			NEXT_OFFSET.getAndSet(Math.abs(UniformDataSource.nextWord(NEXT_OFFSET.get()))),
			size, dataSrc
		);
	}
	//
	public UniformData(final Long offset, final Long size) {
		this(offset, size, UniformDataSource.DEFAULT);
	}
	//
	public UniformData(final Long offset, final Long size, final UniformDataSource dataSrc) {
		this(offset, size, 0, dataSrc);
	}
	//
	public UniformData(
		final Long offset, final Long size, final Integer layerNum, final UniformDataSource dataSrc
	) {
		super(dataSrc.getBytes(layerNum));
		try {
			setOffset(offset, 0);
		} catch(final IOException e) {
			LogUtil.failure(
				LOG, Level.ERROR, e, String.format(FMT_MSG_FAIL_SET_OFFSET, offset)
			);
		}
		this.size = size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final long getOffset() {
		return offset;
	}
	//
	public final void setOffset(final long offset0, final long offset1)
	throws IOException {
		pos = 0;
		offset = offset0 + offset1; // temporary offset
		if(skip(offset % count) == offset % count) {
			offset = offset0;
		} else {
			throw new IOException(String.format(FMT_MSG_FAIL_CHANGE_OFFSET, offset));
		}
	}
	//
	@Override
	public final long getSize() {
		return size;
	}
	//
	@Override
	public final void setSize(final long size) {
		this.size = size;
	}
	//
	@Override
	public final void setDataSource(final DataSource dataSrc, final int overlayIndex) {
		buf = dataSrc.getBytes(overlayIndex);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Ring input stream implementation ////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int available() {
		return size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size;
	}
	//
	@Override
	public final int read() {
		int b;
		do {
			b = super.read();
			if(b < 0) { // end of file
				pos = 0;
			}
		} while(b < 1);
		return b;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int read(final byte buff[])
	throws IOException {
		return read(buff, 0, buff.length);
	}
	//
	@Override
	public final int read(final byte buff[], final int offset, final int length) {
		int doneByteCount = 0, lastByteCount;
		do {
			lastByteCount = super.read(buff, offset + doneByteCount, length - doneByteCount);
			if(lastByteCount < 0) {
				pos = 0;
			} else {
				doneByteCount += lastByteCount;
			}
		} while(doneByteCount < length);
		return doneByteCount;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return String.format(FMT_META_INFO, offset, size);
	}
	//
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		final String tokens[] = v.split(RunTimeConfig.LIST_SEP, 2);
		if(tokens.length==2) {
			try {
				offset = Long.parseLong(tokens[0], 0x10);
			} catch(final NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_OFFSET, tokens[0]));
			}
			try {
				size = Long.parseLong(tokens[1], 10);
			} catch(final NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_SIZE, tokens[1]));
			}
		} else {
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, v));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public int hashCode() {
		return (int) (offset ^ size);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Binary serialization implementation /////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void writeExternal(final ObjectOutput out)
	throws IOException {
		out.writeLong(offset);
		out.writeLong(size);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		setOffset(in.readLong(), 0);
		size = in.readLong();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void writeTo(final OutputStream out)
	throws IOException {
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, FMT_MSG_STREAM_OUT_START, Long.toHexString(offset));
		}
		int
			nextOffset = (int) (offset % buf.length),
			nextLength = buf.length - nextOffset;
		if(nextLength > size) {
			nextLength = (int) size;
		}
		long byteCountLeft = size;
		while(byteCountLeft > 0) {
			out.write(buf, nextOffset, nextLength);
			byteCountLeft -= nextLength;
			nextOffset = 0;
			nextLength = byteCountLeft > buf.length ? buf.length : (int) byteCountLeft;
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, FMT_MSG_STREAM_OUT_FINISH, Long.toHexString(offset));
		}
	}
	//
	private final static String
		FMT_MSG_CONTENT_MISMATCH = "%s: content mismatch @ pos #%d, expected \"0x%X\", but got \"0x%X\"";
	// checks that data read from input equals the specified range
	protected final boolean isContentEqualTo(
		final InputStream in, final long rangeOffset, final long rangeLength
	) throws IOException {
		//
		boolean contentEquals = true;
		long byteCountLeft = rangeLength;
		final byte
			lByteBuff[] = new byte[
				rangeLength > LoadExecutor.BUFF_SIZE_HI ?
					LoadExecutor.BUFF_SIZE_HI :
					rangeLength < LoadExecutor.BUFF_SIZE_LO ?
						LoadExecutor.BUFF_SIZE_LO : (int) rangeLength // cast to int should be safe here
			],
			rByteBuff[] = new byte[lByteBuff.length];
		int nextByteCount;
		//
		while(byteCountLeft > 0 && contentEquals) {
			// determine how many bytes to read in the next iteration
			nextByteCount = byteCountLeft > lByteBuff.length ?
				lByteBuff.length : (int) byteCountLeft;
			if(LOG.isTraceEnabled(LogUtil.MSG)) {
				LOG.trace(
					LogUtil.MSG, "Try to read from remote side the next {} bytes, left: {}",
					nextByteCount, byteCountLeft
				);
			}
			// try to read the determined byte count from the remote side
			int n, m = 0;
			do {
				n = in.read(lByteBuff, m, nextByteCount - m);
				if(n < 0) { // premature end of stream
					contentEquals = false;
					LOG.warn(
						LogUtil.MSG,
						"{}: content size mismatch, expected: {}, got: {}",
						Long.toString(offset, DataObject.ID_RADIX), size,
						rangeOffset + rangeLength - byteCountLeft + m
					);
				} else {
					m += n;
				}
			} while(m < nextByteCount && contentEquals);
			// try to read the determined byte count from the ring buffer
			setOffset(offset, rangeOffset + rangeLength - byteCountLeft);
			if(nextByteCount == read(rByteBuff, 0, nextByteCount)) {
				if(nextByteCount < lByteBuff.length) { // byte count doesn't fit the buffer size
					if(LOG.isTraceEnabled(LogUtil.MSG)) {
						LOG.trace(
							LogUtil.MSG, "Fill the buffers tails from {} to {} w/ equal bytes",
							lByteBuff.length, nextByteCount, lByteBuff.length - 1
						);
					}
					Arrays.fill(lByteBuff, nextByteCount, lByteBuff.length - 1, Byte.MIN_VALUE);
					Arrays.fill(rByteBuff, nextByteCount, lByteBuff.length - 1, Byte.MIN_VALUE);
				}
				// compare the buffers containing the data been read
				if(!Arrays.equals(lByteBuff, rByteBuff)) {
					contentEquals = false;
					// find the 1st corrupted byte and its position
					for(int i = 0; i < nextByteCount; i ++) {
						if(lByteBuff[i] != rByteBuff[i]) {
							LOG.warn(
								LogUtil.MSG,
								String.format(
									FMT_MSG_CONTENT_MISMATCH,
									Long.toString(offset, DataObject.ID_RADIX),
									rangeOffset + rangeLength - byteCountLeft + i,
									rByteBuff[i], lByteBuff[i]
								)
							);
							break;
						}
					}
				}
			} else {
				throw new IllegalStateException(
					String.format("Failed to read %d bytes from ring buffer", nextByteCount)
				);
			}
			// prepare the next iteration
			byteCountLeft -= nextByteCount;
		}
		//
		return contentEquals;
	}
	//
}
