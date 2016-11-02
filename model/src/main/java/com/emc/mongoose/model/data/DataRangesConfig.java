package com.emc.mongoose.model.data;

import com.emc.mongoose.common.api.ByteRange;
import com.emc.mongoose.common.exception.InvalidByteRangeException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 Created by andrey on 02.03.16.
 */
public class DataRangesConfig
implements Serializable {
	
	private final int randomCount;
	private final List<ByteRange> fixedByteRanges;
	
	public DataRangesConfig(final int randomCount) {
		this.randomCount = randomCount;
		this.fixedByteRanges = null;
	}
	
	public DataRangesConfig(final String rawRangesConfig)
	throws InvalidByteRangeException {
		
		randomCount = 0;
		
		final String[] rawRanges;
		if(rawRangesConfig == null || rawRangesConfig.isEmpty()) {
			throw new InvalidByteRangeException("Empty fixed byte range");
		}
		if(rawRangesConfig.contains(",")) {
			rawRanges = rawRangesConfig.split(",");
		} else {
			rawRanges = new String[] { rawRangesConfig };
		}
		
		fixedByteRanges = new ArrayList<>();
		for(final String rawRange : rawRanges) {
			fixedByteRanges.add(new ByteRange(rawRange));
		}
	}

	public DataRangesConfig(final DataRangesConfig other) {
		this.randomCount = other.getRandomCount();
		this.fixedByteRanges = new ArrayList<>(other.getFixedByteRanges());
	}
	
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
	
	public final int getRandomCount() {
		return randomCount;
	}
	
	public final List<ByteRange> getFixedByteRanges() {
		return fixedByteRanges;
	}
}
