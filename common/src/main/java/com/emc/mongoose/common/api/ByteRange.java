package com.emc.mongoose.common.api;

import com.emc.mongoose.common.exception.InvalidByteRangeException;

import java.io.Serializable;

/**
 Created by kurila on 26.09.16.
 */
public final class ByteRange
implements Serializable {
	//
	private final long beg;
	private final long end;
	//
	public ByteRange(final String rawRange)
	throws InvalidByteRangeException {
		if(rawRange.contains("-")) {
			final String[] pair = rawRange.split("-");
			if(pair.length == 2) {
				try {
					beg = Long.parseLong(pair[0]);
					end = Long.parseLong(pair[1]);
				} catch(final NumberFormatException e) {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
				if(beg > end) {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			} else {
				throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
			}
		} else {
			throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
		}
	}
	//
	public final long getBeg() {
		return beg;
	}
	//
	public final long getEnd() {
		return end;
	}
	//
	public final String toString() {
		return Long.toString(beg) + '-' + end;
	}
}
