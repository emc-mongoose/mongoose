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
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer. Not thread safe.
 */
public class UniformData
implements DataItem {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final static String
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: \"%s\"",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: \"%s\"";
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
	private int ringBuffSize;
	protected long offset = 0, size = 0;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformData() {
		setRingBuffer(UniformDataSource.DEFAULT.getLayer(0).asReadOnlyBuffer());
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
		setRingBuffer(dataSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.size = size;
	}
	//
	private void setRingBuffer(final ByteBuffer ringBuff) {
		this.ringBuff = ringBuff;
		ringBuffSize = ringBuff.capacity();
	}
	//
	private void reset() {
		ringBuff.limit(ringBuffSize).position((int) (offset % ringBuffSize));
	}
	//
	private void enforceCircularity() {
		if(!ringBuff.hasRemaining()) {
			ringBuff.clear();
		}
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
	public final void setDataSource(final DataSource dataSrc, final int overlayIndex) {
		setRingBuffer(dataSrc.getLayer(overlayIndex).asReadOnlyBuffer());
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// ReadableByteChannel implementation
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
		enforceCircularity();
		// bytes count to transfer
		final int n = Math.min(dst.remaining(), ringBuff.remaining());
		ringBuff.limit(ringBuff.position() + n);
		// do the transfer
		dst.put(ringBuff);
		return n;
	}
	//
	@Override
	public void write(final WritableByteChannel chanDst)
	throws IOException {
		write(chanDst, 0, size);
	}
	//
	protected final void write(
		final WritableByteChannel chanDst, final long relOffset, final long len
	) throws IOException {
		long writtenCount = 0;
		setRelativeOffset(relOffset);
		while(writtenCount < len) {
			enforceCircularity();
			writtenCount += chanDst.write(ringBuff);
		}
	}
	//
	@Override
	public boolean equals(final ReadableByteChannel chanSrc)
	throws IOException {
		return equals(chanSrc, 0, size);
	}
	// checks that data read from input equals the specified range
	protected final boolean equals(
		final ReadableByteChannel chanSrc, final long relOffset, final long len
	) throws IOException {
		// byte counters
		int n, m;
		// source byte vs input byte
		byte bs, bi;
		//
		final ByteBuffer
			inBuff = ByteBuffer.allocate(
			(int) Math.min(
				LoadExecutor.BUFF_SIZE_HI, Math.max(LoadExecutor.BUFF_SIZE_LO, len)
			)
		);
		long doneByteCount = 0;
		setRelativeOffset(relOffset);
		//
		while(doneByteCount < len) {
			//
			enforceCircularity();
			inBuff.limit(ringBuff.remaining());
			n = chanSrc.read(inBuff);
			//
			if(n < 0) { // premature end of stream
				LOG.warn(
					LogUtil.MSG, "{}: content size mismatch, expected: {}, got: {}",
					Long.toString(offset, DataObject.ID_RADIX), size, relOffset + doneByteCount
				);
				return false;
			} else {
				//
				inBuff.flip();
				//
				for(m = 0; m < n; m ++) {
					bs = ringBuff.get();
					bi = inBuff.get();
					if(bs != bi) {
						LOG.warn(
							LogUtil.MSG, "{}: content mismatch @ offset {}, expected: {}, got: {}",
							Long.toString(offset, DataObject.ID_RADIX),
							relOffset + doneByteCount + m,
							String.format("\"0x%X\"", bs), String.format("\"0x%X\"", bi)
						);
						return false;
					}
				}
				//
				inBuff.clear();
				doneByteCount += n;
			}
		}
		return true;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// Human readable "serialization" implementation ///////////////////////////////////////////////
	////////////////////////////////////////////////////////////////////////////////////////////////
	private final static ThreadLocal<StringBuilder> THR_LOCAL_STR_BUILDER = new ThreadLocal<>();
	//
	@Override
	public String toString() {
		StringBuilder strBuilder = THR_LOCAL_STR_BUILDER.get();
		if(strBuilder == null) {
			strBuilder = new StringBuilder();
			THR_LOCAL_STR_BUILDER.set(strBuilder);
		} else {
			strBuilder.setLength(0); // reset
		}
		return strBuilder.append(Long.toHexString(offset)).append(',').append(size).toString();
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
}
