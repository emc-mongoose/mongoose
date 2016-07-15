package com.emc.mongoose.ui.config;
//
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 Created by andrey on 02.03.16.
 */
public class DataRangesConfig
implements Serializable {
	//
	public final static class InvalidRangeException
	extends IllegalArgumentException {
		public InvalidRangeException(final String msg) {
			super(msg);
		}
	}
	//
	public final static class ByteRange
	implements Serializable {
		//
		private final long beg;
		private final long end;
		//
		public ByteRange(final String rawRange)
		throws InvalidRangeException {
			if(rawRange.contains("-")) {
				final String[] pair = rawRange.split("-");
				if(pair.length == 2) {
					try {
						beg = Long.parseLong(pair[0]);
						end = Long.parseLong(pair[1]);
					} catch(final NumberFormatException e) {
						throw new InvalidRangeException("Invalid range string: \""+ rawRange + "\"");
					}
					if(beg > end) {
						throw new InvalidRangeException("Invalid range string: \""+ rawRange + "\"");
					}
				} else {
					throw new InvalidRangeException("Invalid range string: \""+ rawRange + "\"");
				}
			} else {
				throw new InvalidRangeException("Invalid range string: \""+ rawRange + "\"");
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
	//
	private final int randomCount;
	private final List<ByteRange> fixedByteRanges;
	//
	public DataRangesConfig(final int randomCount) {
		this.randomCount = randomCount;
		this.fixedByteRanges = null;
	}
	//
	public DataRangesConfig(final String rawRangesConfig)
	throws InvalidRangeException {
		//
		randomCount = 0;
		//
		final String[] rawRanges;
		if(rawRangesConfig == null || rawRangesConfig.isEmpty()) {
			throw new InvalidRangeException("Empty fixed byte range");
		}
		if(rawRangesConfig.contains(",")) {
			rawRanges = rawRangesConfig.split(",");
		} else {
			rawRanges = new String[] { rawRangesConfig };
		}
		//
		fixedByteRanges = new ArrayList<>();
		for(final String rawRange : rawRanges) {
			fixedByteRanges.add(new ByteRange(rawRange));
		}
	}
	//
	@Override
	public final String toString() {
		if(randomCount > 0) {
			return Integer.toString(randomCount);
		} else if(fixedByteRanges != null){
			final StringBuilder strb = new StringBuilder();
			for(final ByteRange br : fixedByteRanges) {
				strb.append(br.toString());
			}
			return strb.toString();
		} else {
			return "<NONE>";
		}
	}
	//
	public final int getRandomCount() {
		return randomCount;
	}
	//
	public final List<ByteRange> getFixedByteRanges() {
		return fixedByteRanges;
	}
}
