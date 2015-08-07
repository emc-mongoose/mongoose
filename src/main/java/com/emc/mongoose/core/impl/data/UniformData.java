package com.emc.mongoose.core.impl.data;
// mongoose-common.jar
import com.emc.mongoose.common.conf.Constants;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataCorruptionException;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.DataSizeException;
import com.emc.mongoose.core.api.data.model.DataSource;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.model.UniformDataSource;
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
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s";
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
	protected void reset() {
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
	public final synchronized int read(final ByteBuffer dst) {
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
	public final synchronized int write(final WritableByteChannel chanDst, final long maxCount)
	throws IOException {
		enforceCircularity();
		int n = (int) Math.min(maxCount, ringBuff.remaining());
		ringBuff.limit(ringBuff.position() + n);
		return chanDst.write(ringBuff);
	}
	//
	@Override
	public long writeFully(final WritableByteChannel chanDst)
	throws IOException {
		return writeRange(chanDst, 0, size);
	}
	//
	@Override
	public final long writeRange(
		final WritableByteChannel chanDst, final long relOffset, final long len
	) throws IOException {
		long writtenCount = 0;
		int n;
		setRelativeOffset(relOffset);
		while(writtenCount < len) {
			n = write(chanDst, len-writtenCount);
			if(n < 0) {
				LOG.warn(Markers.ERR, "Channel returned {} as written byte count", n);
			} else if(n > 0) {
				writtenCount += n;
			}
		}
		return writtenCount;
	}
	//
	@Override
	public final int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataSizeException, DataCorruptionException, IOException {
		//
		enforceCircularity();
		int n = ringBuff.remaining();
		if(buff.limit() > n) {
			buff.limit(n);
		}
		//
		n = chanSrc.read(buff);
		//
		if(n < 0) { // premature end of stream
			throw new DataSizeException();
		} else if(n > 0) {
			byte bs, bi;
			buff.flip();
			for(int m = 0; m < n; m ++) {
				bs = ringBuff.get();
				bi = buff.get();
				if(bs != bi) {
					throw new DataCorruptionException(m, bs, bi);
				}
			}
		}
		return n;
	}
	//
	@Override
	public long readAndVerifyFully(final ReadableByteChannel chanSrc)
	throws DataSizeException, DataCorruptionException, IOException {
		return readAndVerifyRange(chanSrc, 0, size);
	}
	// checks that data read from input equals the specified range
	@Override
	public final long readAndVerifyRange(
		final ReadableByteChannel chanSrc, final long relOffset, final long len
	) throws DataSizeException, DataCorruptionException, IOException {
		setRelativeOffset(relOffset);
		final ByteBuffer buff = ByteBuffer.allocate((int) Math.min(Constants.BUFF_SIZE_HI, len));
		//
		int n;
		long doneByteCount = 0;
		try {
			while(doneByteCount < len) {
				n = readAndVerify(chanSrc, buff);
				if(n > 0) {
					doneByteCount += n;
					buff.position(0)
						.limit((int) Math.min(len - doneByteCount, buff.capacity()));
				}
			}
		} catch(final DataSizeException e) {
			e.offset = relOffset + doneByteCount;
			throw e;
		} catch(final DataCorruptionException e) {
			e.offset += doneByteCount;
			throw e;
		}
		return doneByteCount;
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
		return strBuilder
			.append(Long.toHexString(offset)).append(RunTimeConfig.LIST_SEP)
			.append(size).toString();
	}
	//
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		final String tokens[] = v.split(RunTimeConfig.LIST_SEP, 2);
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
