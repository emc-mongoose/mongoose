package com.emc.mongoose.common.api;

import com.emc.mongoose.common.exception.InvalidByteRangeException;

import java.io.Serializable;

import static com.emc.mongoose.common.api.SizeInBytes.toFixedSize;

/**
 Created by kurila on 26.09.16.
 */
public final class ByteRange
implements Serializable {
	
	private final long beg;
	private final long end;
	private final long size;
	
	public ByteRange(final String rawRange)
	throws InvalidByteRangeException, NumberFormatException {
		if(rawRange.startsWith("-")) {
			if(rawRange.endsWith("-")) {
				if(rawRange.length() > 2) {
					beg = end = -1;
					size = toFixedSize(rawRange.substring(1, rawRange.length() - 1));
				} else {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			} else {
				if(rawRange.length() > 1) {
					beg = -1;
					end = toFixedSize(rawRange.substring(1));
					size = -1;
				} else {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			}
		} else if(rawRange.endsWith("-")) {
			if(rawRange.length() > 1) {
				beg = toFixedSize(rawRange.substring(0, rawRange.length() - 1));
				end = -1;
				size = -1;
			} else {
				throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
			}
		} else if(rawRange.contains("-")){
			final String[] pair = rawRange.split("-");
			if(pair.length == 2) {
				beg = toFixedSize(pair[0]);
				end = toFixedSize(pair[1]);
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
