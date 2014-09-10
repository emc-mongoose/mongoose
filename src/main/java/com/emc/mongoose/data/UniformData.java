package com.emc.mongoose.data;
//
import com.emc.mongoose.conf.RunTimeConfig;
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
			setOffset(offset);
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
	public final void setOffset(final long offset)
	throws IOException {
		pos = 0;
		if(skip(offset % count)==offset % count) {
			this.offset = offset;
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
	private final static class NullOutputStream
	extends OutputStream {
		@Override
		public void write(final int b) {}
	}
	//
	/** [Re]calculates the checkSum field with ranges */
	public final void calcCheckSum() {
		UniformData nextRange;
		long spaceBeforeNextRange, freeSpaceOffset = 0;
		synchronized(md) {
			try(final DigestOutputStream outDigest = new DigestOutputStream(new NullOutputStream(), md)) {
				for(final long nextRangeOffset : ranges.keySet()) {
					if(nextRangeOffset > freeSpaceOffset) {
						spaceBeforeNextRange = nextRangeOffset - freeSpaceOffset;
						writeBlockTo(outDigest, freeSpaceOffset, spaceBeforeNextRange);
					}
					nextRange = ranges.get(nextRangeOffset);
					nextRange.writeBlockTo(outDigest, 0, nextRange.size);
					freeSpaceOffset = nextRangeOffset + nextRange.size;
				}
				if(freeSpaceOffset < size) {
					writeBlockTo(outDigest, freeSpaceOffset, size);
				}
				checkSum = Base64.encodeBase64URLSafeString(md.digest());
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
		return
			Long.toHexString(offset) + RunTimeConfig.LIST_SEP + Long.toHexString(size) +
			((ranges!=null && ranges.size()>0) ? ranges.toString() : "");
	}
	//
	public void fromString(final String v)
	throws IllegalArgumentException, NullPointerException {
		String t;
		int nextSepPos = v.indexOf(RunTimeConfig.LIST_SEP);
		if(nextSepPos > 0 && nextSepPos + 1 < v.length()) {
			offset = Long.parseLong(v.substring(0, nextSepPos), 0x10);
			t = v.substring(nextSepPos + 1);
			nextSepPos = t.indexOf(RunTimeConfig.LIST_SEP);
			if(nextSepPos > 0 && nextSepPos + 1 < v.length()) {
				size = Long.parseLong(t.substring(0, nextSepPos), 0x10);
				ranges.fromString(t.substring(nextSepPos + 1));
			} else {
				size = Long.parseLong(t, 0x10);
			}
		} else {
			throw new IllegalArgumentException("Invalid data item metainfo: "+v);
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
		setOffset(in.readLong());
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
			setOffset(offset + start); // resets the position in the ring to the beginning of the item
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
			Markers.MSG, "checksum verification for {}, assumed: \"{}\", read back: \"{}\"",
			toString(), checkSum, readCheckSum
		);
		return checkSum.equals(readCheckSum);
	}
	//
}
