package com.emc.mongoose.core.impl.data;
//
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.DataObject;
import com.emc.mongoose.core.api.data.src.DataSource;
//
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
import com.emc.mongoose.core.impl.data.src.UniformDataSource;
//
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer.
 */
public class UniformData
extends InputStream
implements DataItem {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"",
		FMT_MSG_STREAM_OUT_START = "Item \"{}\": stream out start",
		FMT_MSG_STREAM_OUT_FINISH = "Item \"{}\": stream out finish";
	protected final static String
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s",
		MSG_READ_RING_BLOCKED = "Reading from data ring blocked?";
	private static AtomicLong
		LAST_OFFSET = new AtomicLong(
			Math.abs(
				Long.reverse(System.currentTimeMillis()) ^
				Long.reverseBytes(System.nanoTime()) ^
				ServiceUtils.getHostAddrCode()
			)
		);
	public static long nextOffset(final AtomicLong lastOffset) {
		return lastOffset.getAndSet(
			Math.abs(
				UniformDataSource.nextWord(lastOffset.get()) ^ System.nanoTime()
			)
		);
	}
	//
	private ByteBuffer ringBuff;
	private int ringBuffOffset, ringBuffSize;
	protected long offset = 0, size = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformData() {
		setRingBuffer(UniformDataSource.DEFAULT.getLayer(0));
		setOffset(nextOffset(LAST_OFFSET));
	}
	//
	public UniformData(final String metaInfo) {
		this();
		fromString(metaInfo);
	}
	//
	public UniformData(final Long size) {
		this(nextOffset(LAST_OFFSET), size, UniformDataSource.DEFAULT);
	}
	//
	public UniformData(final Long size, final UniformDataSource dataSrc) {
		this(nextOffset(LAST_OFFSET), size, dataSrc);
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
		setRingBuffer(dataSrc.getLayer(layerNum));
		setOffset(offset);
		this.size = size;
	}
	//
	private void setRingBuffer(final ByteBuffer ringBuff) {
		this.ringBuff = ringBuff;
		ringBuffSize = ringBuff.capacity();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final long getOffset() {
		return offset;
	}
	//
	@Override
	public final void setOffset(final long offset) {
		this.offset = offset;
		ringBuffOffset = (int) (offset % ringBuffSize);
		reset();
	}
	//
	public final int getRelativeOffset() {
		return ringBuff.position();
	}
	//
	public final void setRelativeOffset(final long relOffset) {
		ringBuff.clear().position((int) ((offset + relOffset) % ringBuffSize));
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
	public final void setDataSource(final DataSource dataSrc, final int overlayIndex) {
		setRingBuffer(dataSrc.getLayer(overlayIndex));
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
	public final void reset() {
		ringBuff.clear().position(ringBuffOffset);
	}
	//
	@Override
	public final int read() {
		if(!ringBuff.hasRemaining()) {
			ringBuff.flip();
		}
		return ringBuff.get();
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
		int nextLen, doneLen = 0;
		while(doneLen < length) {
			if(!ringBuff.hasRemaining()) {
				ringBuff.clear();
			}
			nextLen = Math.min(ringBuff.remaining(), length - doneLen);
			ringBuff.get(buff, offset + doneLen, nextLen);
			doneLen += nextLen;
		}
		return length;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public String toString() {
		return Long.toHexString(offset) + "," + size;
	}
	//
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		final String tokens[] = v.split(",", 2);
		if(tokens.length == 2) {
			try {
				setOffset(Long.parseLong(tokens[0], 0x10));
			} catch(final NumberFormatException e) {
				throw new IllegalArgumentException(String.format(FMT_MSG_OFFSET, tokens[0]));
			}
			try {
				setSize(Long.parseLong(tokens[1], 10));
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
		setOffset(in.readLong());
		setSize(in.readLong());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public void writeTo(final OutputStream out)
	throws IOException {
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, FMT_MSG_STREAM_OUT_START, Long.toHexString(offset));
		}
		long doneLen = 0;
		final WritableByteChannel chan = Channels.newChannel(out);
		int nextLimit;
		if(ringBuff.position() != ringBuffOffset) {
			reset();
		}
		while(doneLen < size) {
			if(!ringBuff.hasRemaining()) {
				ringBuff.clear();
			}
			nextLimit = (int) Math.min(ringBuff.remaining(), size - doneLen);
			ringBuff.limit(ringBuff.position() + nextLimit);
			doneLen += chan.write(ringBuff);
		}
		if(LOG.isTraceEnabled(LogUtil.MSG)) {
			LOG.trace(LogUtil.MSG, FMT_MSG_STREAM_OUT_FINISH, Long.toHexString(offset));
		}
	}
	// checks that data read from input equals the specified range
	protected boolean isContentEqualTo(final InputStream in, final long rOffset, final long length)
	throws IOException {
		//
		final byte
			inBuff[] = new byte[
				(int) Math.min(
					LoadExecutor.BUFF_SIZE_HI, Math.max(LoadExecutor.BUFF_SIZE_LO, length)
				)
			];
		setRelativeOffset(rOffset);
		long doneByteCount = 0, nextByteCount;
		int n, m;
		byte b;
		while(doneByteCount < length) {
			if(!ringBuff.hasRemaining()) {
				ringBuff.clear();
			}
			nextByteCount = Math.min(length - doneByteCount, ringBuff.remaining());
			nextByteCount = Math.min(nextByteCount, inBuff.length);
			n = in.read(inBuff, 0, (int) nextByteCount); // cast should be safe here
			if(n < 0) { // premature end of stream
				LOG.warn(
					LogUtil.MSG, "{}: content size mismatch, expected: {}, got: {}",
					Long.toString(offset, DataObject.ID_RADIX), size, rOffset + doneByteCount
				);
				return false;
			} else {
				for(m = 0; m < n; m ++) {
					b = ringBuff.get();
					if(b != inBuff[m]) {
						LOG.warn(
							LogUtil.MSG, "{}: content mismatch @ offset {}, expected: {}, got: {}",
							Long.toString(offset, DataObject.ID_RADIX), rOffset + doneByteCount + m,
							String.format("\"0x%X\"", b), String.format("\"0x%X\"", inBuff[m])
						);
						return false;
					}
				}
				doneByteCount += n;
			}
		}
		return true;
	}
	//
}
