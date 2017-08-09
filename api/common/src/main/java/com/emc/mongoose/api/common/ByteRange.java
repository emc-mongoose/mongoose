package com.emc.mongoose.api.common;

import com.emc.mongoose.api.common.exception.InvalidByteRangeException;

/**
 Created by kurila on 26.09.16.
 */
public final class ByteRange {
	
	private final long beg;
	private final long end;
	private final long size;
	
	public ByteRange(final long beg, final long end, final long size) {
		this.beg = beg;
		this.end = end;
		this.size = size;
	}
	
	public ByteRange(final String rawRange)
	throws InvalidByteRangeException, NumberFormatException {
		if(rawRange.startsWith("-")) {
			if(rawRange.endsWith("-")) {
				if(rawRange.length() > 2) {
					beg = end = -1;
					size = SizeInBytes.toFixedSize(rawRange.substring(1, rawRange.length() - 1));
				} else {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			} else {
				if(rawRange.length() > 1) {
					beg = -1;
					end = SizeInBytes.toFixedSize(rawRange.substring(1));
					size = -1;
				} else {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			}
		} else if(rawRange.endsWith("-")) {
			if(rawRange.length() > 1) {
				beg = SizeInBytes.toFixedSize(rawRange.substring(0, rawRange.length() - 1));
				end = -1;
				size = -1;
			} else {
				throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
			}
		} else if(rawRange.contains("-")){
			final String[] pair = rawRange.split("-");
			if(pair.length == 2) {
				beg = SizeInBytes.toFixedSize(pair[0]);
				end = SizeInBytes.toFixedSize(pair[1]);
				size = -1;
			} else {
				throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
			}
		} else {
			throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
		}
	}

	public final long getBeg() {
		return beg;
	}
	
	public final long getEnd() {
		return end;
	}

	/**
	 Note that this method may return -1 if begin and end are set (size is not -1 actually)
	 @return the size of the range having no position
	 */
	public final long getSize() {
		return size;
	}
	
	public final String toString() {
		if(beg == -1) {
			if(end == -1) {
				return "-" + size + "-";
			}
			return "-" + end;
		} else if(end == -1) {
			return beg + "-";
		} else {
			return beg + "-" + end;
		}
	}
}
