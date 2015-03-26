package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.object.data.DataObject;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.TraceLogger;
import com.emc.mongoose.util.logging.Markers;
import com.emc.mongoose.util.remote.ServiceUtils;
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
	public final static int maxBuffSize = (int) RunTimeConfig.getContext().getDataPageSize();
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
			TraceLogger.failure(
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
	public final void setDataSource(final UniformDataSource dataSrc, final int layerNum) {
		buf = dataSrc.getBytes(layerNum);
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
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, FMT_MSG_STREAM_OUT_START, Long.toHexString(offset));
		}
		long byteCountToWrite = size;
		int
			nextOffset = (int) (offset % buf.length),
			nextLength = buf.length - nextOffset;
		while(byteCountToWrite > 0) {
			if(byteCountToWrite < nextLength) { // tail bytes case
				nextLength = (int) byteCountToWrite;
			}
			// write the determined bytes range
			out.write(buf, nextOffset, nextLength);
			// prepare the next iteration
			byteCountToWrite -= nextLength;
			nextOffset = 0;
			nextLength = buf.length;
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, FMT_MSG_STREAM_OUT_FINISH, Long.toHexString(offset));
		}
	}
	// checks that data read from input equals the specified range
	protected final boolean isContentEqualTo(
		final InputStream in, final long rangeOffset, final long rangeLength
	) throws IOException {
		//
		boolean contentEquals = true;
		long byteCountToRead = rangeLength;
		int
			nextOffset = (int) (offset + rangeOffset) % buf.length,
			nextLength = buf.length - nextOffset,
			nextReadByteCount, nextReadByteCountSum;
		final byte buff2verify[] = new byte[buf.length];
		while(byteCountToRead > 0) {
			if(byteCountToRead < nextLength) { // tail bytes case
				nextLength = (int) byteCountToRead;
			}
			// read the determined bytes range
			nextReadByteCountSum = 0;
			do {
				nextReadByteCount = in.read(
					buff2verify, nextReadByteCountSum, nextLength - nextReadByteCountSum
				);
				if(nextReadByteCount < 0) {
					contentEquals = false;
					LOG.warn(
						Markers.MSG,
						"{}: data corruption, expected size: {}, got: {}",
						Long.toString(offset, DataObject.ID_RADIX), size,
						rangeOffset + rangeLength - byteCountToRead + nextReadByteCountSum
					);
					break;
				} else {
					nextReadByteCountSum += nextReadByteCount;
				}
			} while(nextReadByteCountSum < nextLength);
			if(!contentEquals) {
				break;
			}
			// verify the data
			for(int i = 0; i < nextLength; i ++) {
				if(buf[nextOffset + i] != buff2verify[i]) {
					contentEquals = false;
					LOG.warn(
						Markers.MSG,
						"{}: data corruption, size: {}, offset: {}, expected value: {}, got: {}",
						Long.toString(offset, DataObject.ID_RADIX), size,
						rangeOffset - byteCountToRead + i,
						Integer.toHexString(buf[nextOffset + i]),
						Integer.toHexString(buff2verify[i])
					);
					break;
				}
			}
			if(!contentEquals) {
				break;
			}
			// prepare the next iteration
			byteCountToRead -= nextLength;
			nextOffset = 0;
			nextLength = buf.length;
		}
		//
		return contentEquals;
	}
	//
}
