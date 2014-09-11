package com.emc.mongoose.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
import com.emc.mongoose.data.persist.NullOutputStream;
import com.emc.mongoose.logging.Markers;
import com.emc.mongoose.remote.ServiceUtils;
//
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
//
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import java.io.ByteArrayInputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.concurrent.atomic.AtomicLong;
/**
 Created by kurila on 09.05.14.
 */
public class UniformData
extends ByteArrayInputStream
implements Externalizable {
	//
	private final static Logger LOG = LogManager.getLogger();
	private final static String
		FMT_META_INFO = "%x" + RunTimeConfig.LIST_SEP + "%x" + RunTimeConfig.LIST_SEP + "%s",
		FMT_MSG_OFFSET = "Data item offset is not correct hexadecimal value: %s",
		FMT_MSG_SIZE = "Data item size is not correct hexadecimal value: %s",
		FMT_MSG_CHECKSUM = "Date item checksum is not Base64 value: %s",
		FMT_MSG_INVALID_RECORD = "Invalid data item meta info: %s";
	//
	public final static int MAX_PAGE_SIZE = (int) RunTimeConfig.getSizeBytes("data.page.size");
	private static AtomicLong NEXT_OFFSET = new AtomicLong(
		Math.abs(System.nanoTime() ^ ServiceUtils.getHostAddrCode())
	);
	//
	protected final MessageDigest md = DigestUtils.getMd5Digest();
	protected long offset = 0;
	protected long size = 0;
	protected Ranges ranges = null;
	protected String checkSum = null;
	////////////////////////////////////////////////////////////////////////////////////////////////
	public UniformData() {
		super(UniformDataSource.DEFAULT.getBytes());
		ranges = new Ranges(size);
	}
	//
	public UniformData(final String metaInfo) {
		this();
		fromString(metaInfo);
	}
	//
	public UniformData(final long size) {
		this(
			NEXT_OFFSET.getAndSet(
				Math.abs(UniformDataSource.nextWord(NEXT_OFFSET.get()))
			),
			size
		);
	}
	//
	public UniformData(final long offset, final long size) {
		super(UniformDataSource.DEFAULT.getBytes());
		try {
			setOffset(offset, 0);
		} catch(final IOException e) {
			LOG.error(Markers.ERR, "Failed to set data ring offset: {}: {}", offset, e.toString());
			if(LOG.isTraceEnabled()) {
				final Throwable cause = e.getCause();
				if(cause!=null) {
					LOG.trace(Markers.ERR, cause.toString(), cause.getCause());
				}
			}
		}
		this.size = size;
		ranges = new Ranges(size);
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public Ranges getRanges() {
		return ranges;
	}
	//
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
	/**
	 Gets the data checksum which is calculated during read.
	 Should be called <u>once</u> only after the data has being read.
	 @return MD5 checksum encoded as URL-safe-base64 string */
	public final String getCheckSum() {
		if(checkSum==null) {
			checkSum = Base64.encodeBase64URLSafeString(md.digest());
		}
		return checkSum;
	}
	//
	public final void setCheckSum(final String checkSum) {
		this.checkSum = checkSum;
	}
	//
	/** [Re]calculates the checkSum field with ranges */
	public final void calcCheckSum() {
		UniformData nextRange;
		long freeSpaceOffset = 0, lenFreeSpace;
		synchronized(md) {
			final DigestOutputStream outDigest = new DigestOutputStream(new NullOutputStream(), md);
			try {
				for(final long nextRangeOffset : ranges.keySet()) {
					if(nextRangeOffset > freeSpaceOffset) {
						lenFreeSpace = nextRangeOffset - freeSpaceOffset;
						/*LOG.trace(
							Markers.MSG, "digest: base range [{}-{}]",
							freeSpaceOffset, freeSpaceOffset+lenFreeSpace-1
						);*/
						writeBlockTo(outDigest, freeSpaceOffset, lenFreeSpace);
					}
					nextRange = ranges.get(nextRangeOffset);
					/*LOG.trace(
						Markers.MSG, "digest: modified range [{}-{}]",
						nextRangeOffset, nextRangeOffset+nextRange.size-1
					);*/
					nextRange.writeBlockTo(outDigest, 0, nextRange.size);
					freeSpaceOffset = nextRangeOffset + nextRange.size;
				}
				if(freeSpaceOffset < size) {
					lenFreeSpace = size - freeSpaceOffset;
					/*LOG.trace(
						Markers.MSG, "digest: LAST base range [{}-{}]",
						freeSpaceOffset, freeSpaceOffset+lenFreeSpace-1
					);*/
					writeBlockTo(outDigest, freeSpaceOffset, lenFreeSpace);
				}
				checkSum = Base64.encodeBase64URLSafeString(md.digest());
				/*LOG.trace(
					Markers.MSG, "digest for \"{}\" is \"{}\"",
					Hex.encodeHexString(byteByff.toByteArray()), checkSum
				);*/
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, e.getMessage());
			}
		}
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
		return String.format(FMT_META_INFO, offset, size, checkSum);
	}
	//
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		final String tokens[] = v.split(RunTimeConfig.LIST_SEP, 3);
		if(tokens.length==3) {
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
			checkSum = tokens[2];
			if(!Base64.isBase64(checkSum)) {
				throw new IllegalArgumentException(String.format(FMT_MSG_CHECKSUM, checkSum));
			}
		} else {
			throw new IllegalArgumentException(String.format(FMT_MSG_INVALID_RECORD, v));
		}
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final int hashCode() {
		return (int) (offset + size + (ranges==null ? 0 : ranges.size()));
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
		this.size = in.readLong();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	public final void writeTo(final OutputStream out) {
		synchronized(md) {
			try {
				writeBlockTo(
					new DigestOutputStream(out, md),
					0, size
				);
			} catch(final IOException e) {
				LOG.error(Markers.ERR, e.getMessage());
			}
		}
		checkSum = Base64.encodeBase64URLSafeString(md.digest());
	}
	//
	private void writeBlockTo(final OutputStream out, final long start, final long length)
	throws IOException {
		final byte buff[] = new byte[(int)size % MAX_PAGE_SIZE];
		final int
			countPages = (int) length / buff.length,
			countTailBytes = (int) length % buff.length;
		synchronized(this) {
			setOffset(offset, start); // resets the position in the ring to the beginning of the item
			//
			for(int i = 0; i < countPages; i++) {
				if(read(buff)==buff.length) {
					out.write(buff);
				} else {
					throw new InterruptedIOException("Reading from data ring blocked");
				}
			}
			// tail bytes
			if(read(buff, 0, countTailBytes)==countTailBytes) {
				out.write(buff, 0, countTailBytes);
			} else {
				throw new InterruptedIOException("Reading from data ring blocked");
			}
		}
	}
	//
	public final boolean compareWithData(final InputStream in) {
		if(checkSum==null) {
			throw new IllegalStateException("No checksum to verify");
		}
		//
		final String readCheckSum;
		final byte buff[] = new byte[(int) size % MAX_PAGE_SIZE];
		int nextByteCount;
		//
		synchronized(md) {
			try(final DigestInputStream inDigest = new DigestInputStream(in, md);) {
				do {
					nextByteCount = inDigest.read(buff);
				} while(nextByteCount >= 0);
			} catch(final IOException e) {
				LOG.warn(Markers.ERR, "Failed to read the data");
			}
			readCheckSum = Base64.encodeBase64URLSafeString(md.digest());
		}
		LOG.trace(
			Markers.MSG, "Checksum verification for {}, read back data checksum: \"{}\"",
			toString(), readCheckSum
		);
		return checkSum.equals(readCheckSum);
	}
	//
}
