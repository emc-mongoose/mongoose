package com.emc.mongoose.common.api;

import com.emc.mongoose.common.exception.InvalidByteRangeException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by kurila on 26.09.16.
 */
public final class ByteRange
implements Serializable {
	
	private final long beg;
	private final long end;
	
	public ByteRange(final String rawRange)
	throws InvalidByteRangeException {
		if(rawRange.contains("-")) {
			final String[] pair = rawRange.split("-");
			if(pair.length == 2) {
				try {
					if(pair[0].isEmpty()) { // append support
						beg = - 1;
					} else {
						beg = SizeInBytes.toFixedSize(pair[0]);
					}
					end = SizeInBytes.toFixedSize(pair[1]);
				} catch(final NumberFormatException e) {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
				if(beg > end && end != -1) {
					throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			} else if(pair.length == 1) {
				beg = SizeInBytes.toFixedSize(pair[0]);
				end = -1;
			} else {
				throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
			}
		} else {
			throw new InvalidByteRangeException("Invalid range string: \""+ rawRange + "\"");
		}
	}

	public static List<ByteRange> parseList(final String rawRangesStr) {

		if(rawRangesStr == null || rawRangesStr.isEmpty()) {
			return null;
		}

		final String[] rawRanges;
		if(rawRangesStr.contains(",")) {
			rawRanges = rawRangesStr.split(",");
		} else {
			rawRanges = new String[] { rawRangesStr };
		}

		final List<ByteRange> fixedByteRanges = new ArrayList<>();
		for(final String rawRange : rawRanges) {
			fixedByteRanges.add(new ByteRange(rawRange));
		}

		return fixedByteRanges;
	}
	
	public final long getBeg() {
		return beg;
	}
	
	public final long getEnd() {
		return end;
	}
	
	public final String toString() {
		if(beg == -1) {
			return "-" + end;
		} else if(end == -1) {
			return beg + "-";
		} else {
			return beg + "-" + end;
		}
	}
}
