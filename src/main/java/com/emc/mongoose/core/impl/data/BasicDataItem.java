package com.emc.mongoose.core.impl.data;
// mongoose-common.jar
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.net.ServiceUtil;
// mongoose-core-api.jar
import com.emc.mongoose.core.api.data.DataCorruptionException;
import com.emc.mongoose.core.api.data.DataItem;
import com.emc.mongoose.core.api.data.content.ContentSource;
// mongoose-core-impl.jar
import com.emc.mongoose.core.impl.data.content.ContentSourceBase;
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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 09.05.14.
 A data item which may produce uniformly distributed non-compressible content.
 Uses UniformDataSource as a ring buffer. Not thread safe.
 */
public class BasicDataItem
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
				ServiceUtil.getHostAddrCode()
			)
		);
	public static long nextOffset(final AtomicLong lastOffset) {
		return lastOffset.getAndSet(
			Math.abs(
				ContentSourceBase.nextWord(lastOffset.get()) ^ System.nanoTime()
			)
		);
	}
	//
	private ByteBuffer ringBuff;
	private int ringBuffSize;
	protected long offset = 0, size = 0;
	protected String name = null;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public BasicDataItem() {
		this(
			ContentSourceBase.DEFAULT == null ?
				ContentSourceBase.getDefault() : ContentSourceBase.DEFAULT
		);
	}
	//
	public BasicDataItem(final ContentSource contentSrc) {
		setRingBuffer(contentSrc.getLayer(0).asReadOnlyBuffer());
		setOffset(nextOffset(LAST_OFFSET));
	}
	//
	public BasicDataItem(final String metaInfo, final ContentSource contentSrc) {
		this(contentSrc);
		final String tokens[] = metaInfo.split(RunTimeConfig.LIST_SEP, 3);
		if(tokens.length == 3) {
			name = tokens[0];
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
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, metaInfo));
		}
	}
	//
	public BasicDataItem(final Long size, final ContentSource contentSrc) {
		this(nextOffset(LAST_OFFSET), size, contentSrc);
	}
	//
	public BasicDataItem(final Long offset, final Long size, final ContentSource contentSrc) {
		this(offset, size, 0, contentSrc);
	}
	//
	public BasicDataItem(
		final Long offset, final Long size, final Integer layerNum, final ContentSource contentSrc
	) {
		setRingBuffer(contentSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.size = size;
	}
	//
	public BasicDataItem(
		final String name, final Long offset, final Long size, final Integer layerNum,
		final ContentSource contentSrc
	) {
		setRingBuffer(contentSrc.getLayer(layerNum).asReadOnlyBuffer());
		setOffset(offset);
		this.name = name;
		this.size = size;
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final String getName() {
		return name;
	}
	//
	@Override
	public final void setName(final String name) {
		this.name = name;
	}
	//
	private void setRingBuffer(final ByteBuffer ringBuff) {
		this.ringBuff = ringBuff;
		ringBuffSize = ringBuff.capacity();
	}
	//
	private void enforceCircularity() {
		if(!ringBuff.hasRemaining()) {
			ringBuff.clear();
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public void reset() {
		ringBuff.limit(ringBuffSize).position((int) (offset % ringBuffSize));
	}
	//
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
	public final void setContentSource(final ContentSource dataSrc, final int overlayIndex) {
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
	public final int readAndVerify(final ReadableByteChannel chanSrc, final ByteBuffer buff)
	throws DataCorruptionException, IOException {
		//
		enforceCircularity();
		int n = ringBuff.remaining();
		if(buff.limit() > n) {
			buff.limit(n);
		}
		//
		n = chanSrc.read(buff);
		//
		if(n > 0) {
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
			.append(name).append(RunTimeConfig.LIST_SEP)
			.append(Long.toHexString(offset)).append(RunTimeConfig.LIST_SEP)
			.append(size).toString();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public boolean equals(final Object o) {
		if (o == this) {
			return true;
		}
		if (!(o instanceof BasicDataItem)) {
			return false;
		}
		final BasicDataItem other = BasicDataItem.class.cast(o);
		return (size == other.size) && (offset == other.offset);
	}
	//
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
		final byte nameBytes[] = name.getBytes(StandardCharsets.UTF_8);
		out.writeInt(nameBytes.length);
		out.write(nameBytes, 0, nameBytes.length);
		out.writeLong(offset);
		out.writeLong(size);
	}
	//
	@Override
	public void readExternal(final ObjectInput in)
	throws IOException, ClassNotFoundException {
		final byte nameBytes[] = new byte[in.readInt()];
		in.readFully(nameBytes);
		name = new String(nameBytes, StandardCharsets.UTF_8);
		setOffset(in.readLong());
		setSize(in.readLong());
	}
}
