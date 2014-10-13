package com.emc.mongoose.base.data.impl;
//
import com.emc.mongoose.base.data.DataItem;
import com.emc.mongoose.run.Main;
import com.emc.mongoose.util.conf.RunTimeConfig;
import com.emc.mongoose.util.logging.ExceptionHandler;
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
import java.io.InterruptedIOException;
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
	private final static String
		FMT_META_INFO = "%x" + RunTimeConfig.LIST_SEP + "%x",
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: %s",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: %s";
	protected final static String
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s";
	private static AtomicLong NEXT_OFFSET = new AtomicLong(
		Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
	);
	//
	private final int maxPageSize = (int) Main.RUN_TIME_CONFIG.getDataPageSize();
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
	public UniformData(final long size) {
		this(size, UniformDataSource.DEFAULT);
	}
	//
	public UniformData(final long size, final UniformDataSource dataSrc) {
		this(
			NEXT_OFFSET.getAndSet(Math.abs(UniformDataSource.nextWord(NEXT_OFFSET.get()))),
			size, dataSrc
		);
	}
	//
	public UniformData(final long offset, final long size) {
		this(offset, size, UniformDataSource.DEFAULT);
	}
	//
	public UniformData(final long offset, final long size, final UniformDataSource dataSrc) {
		this(offset, size, 0, dataSrc);
	}
	//
	public UniformData(
		final long offset, final long size, final int layerNum, final UniformDataSource dataSrc
	) {
		super(dataSrc.getBytes(layerNum));
		try {
			setOffset(offset, 0);
		} catch(final IOException e) {
			ExceptionHandler.trace(
				LOG, Level.ERROR, e, String.format("Failed to set data ring offset: %s", offset)
			);
		}
		this.size = size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final long getOffset() {
		return offset;
	}
	//
	public final void setOffset(final long offset0, final long offset1)
	throws IOException {
		pos = 0;
		offset = offset0 + offset1; // temporary offset
		if(skip(offset % count)==offset % count) {
			offset = offset0;
		} else {
			throw new IOException(
				"Failed to change offset to \""+Long.toHexString(offset)+"\""
			);
		}
	}
	//
	public final long getSize() {
		return size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Ring input stream implementation ////////////////////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int available() {
		return (int) size;
	}
	//
	@Override
	public final int read() {
		int b = super.read();
		if(b<0) { // end of file
			pos = 0;
			b = super.read(); // re-read the byte
		}
		return b;
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int read(final byte buff[])
	throws IOException {
		return read(buff, 0, buff.length);
	}
	//
	@Override @SuppressWarnings("NullableProblems")
	public final int read(final byte buff[], final int offset, final int length) {
		int doneByteCount = super.read(buff, offset, length);
		if(doneByteCount < length) {
			if(doneByteCount==-1) {
				doneByteCount = 0;
			}
			pos = 0;
			doneByteCount += super.read(buff, offset + doneByteCount, length - doneByteCount);
		}
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
				size = Long.parseLong(tokens[1], 0x10);
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
	public final void writeTo(final OutputStream out) {
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Item \"{}\": stream out start", Long.toHexString(offset));
		}
		final byte buff[] = new byte[size < maxPageSize ? (int) size : maxPageSize];
		final int
			countPages = (int) size / buff.length,
			countTailBytes = (int) size % buff.length;
		synchronized(this) {
			try {
				setOffset(offset, 0); // resets the position in the ring to the beginning of the item
				//
				for(int i = 0; i < countPages; i++) {
					if(read(buff)==buff.length) {
						out.write(buff);
					} else {
						throw new InterruptedIOException("Reading from data ring blocked?");
					}
				}
				// tail bytes
				if(countTailBytes > 0) {
					if(read(buff, 0, countTailBytes)==countTailBytes) {
						out.write(buff, 0, countTailBytes);
					} else {
						throw new InterruptedIOException("Reading from data ring blocked?");
					}
				}
			} catch(final IOException e) {
				LOG.error(Markers.ERR, e.getMessage());
			}
		}
		if(LOG.isTraceEnabled(Markers.MSG)) {
			LOG.trace(Markers.MSG, "Item \"{}\": stream out finish", Long.toHexString(offset));
		}
	}
	// checks that data read from input equals the specified range
	protected final boolean compareWith(
		final InputStream in, final long rangeOffset, final int rangeLength
	) {
		//
		boolean contentEquals = true;
		final byte
			buff1[] = new byte[rangeLength < maxPageSize ? rangeLength : maxPageSize],
			buff2[] = new byte[buff1.length];
		final int
			countPages = rangeLength / buff1.length,
			countTailBytes = rangeLength % buff1.length;
		int doneByteCountSum, doneByteCount;
		//
		synchronized(this) {
			try {
				setOffset(offset, rangeOffset);
				for(int i = 0; i < countPages; i++) {
					if(buff1.length==read(buff1)) {
						doneByteCountSum = 0;
						do {
							doneByteCount = in.read(
								buff2, doneByteCountSum, buff2.length - doneByteCountSum
							);
							if(doneByteCount < 0) {
								break;
							} else {
								doneByteCountSum += doneByteCount;
							}
						} while(doneByteCountSum < buff2.length);
						contentEquals = Arrays.equals(buff1, buff2);
						if(!contentEquals) {
							break;
						}
					} else {
						LOG.debug(Markers.ERR, "Looks like reading from data ring blocked");
						contentEquals = false;
						break;
					}
				}
				//
				if(contentEquals && countTailBytes > 0) {
					// tail bytes
					if(read(buff1, 0, countTailBytes)==countTailBytes) {
						doneByteCountSum = 0;
						do {
							doneByteCount = in.read(
								buff2, doneByteCountSum, countTailBytes - doneByteCountSum
							);
							if(doneByteCount < 0) {
								break;
							} else {
								doneByteCountSum += doneByteCount;
							}
						} while(doneByteCountSum < countTailBytes);
						contentEquals = Arrays.equals(buff1, buff2);
					} else {
						LOG.debug(Markers.ERR, "Looks like reading from data ring blocked");
						contentEquals = false;
					}
				}
			} catch(final IOException e) {
				contentEquals = false;
				ExceptionHandler.trace(LOG, Level.WARN, e, "Data integrity verification failure");
			}
		}
		//
		return contentEquals;
	}
}
