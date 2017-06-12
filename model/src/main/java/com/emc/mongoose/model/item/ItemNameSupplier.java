package com.emc.mongoose.model.item;

import com.emc.mongoose.common.exception.DanShootHisFootException;
import com.emc.mongoose.common.math.MathUtil;
import com.emc.mongoose.common.net.NetUtil;
import com.emc.mongoose.common.supply.BasicUpdatingValueSupplier;

import static java.lang.System.currentTimeMillis;
import static java.lang.System.nanoTime;

/**
 Created by kurila on 18.12.15.
 */
public class ItemNameSupplier
extends BasicUpdatingValueSupplier<String>
implements IdStringInput {
	//
	protected final ItemNamingType namingType;
	protected final int length, prefixLength, radix;
	protected final StringBuilder strb = new StringBuilder();
	//
	protected volatile long lastValue;
	//
	public ItemNameSupplier(
		final ItemNamingType namingType, final String prefix, final int length, final int radix,
		final long offset
	) throws DanShootHisFootException {
		//
		super(null, null);
		//
		if(namingType != null) {
			this.namingType = namingType;
		} else {
			this.namingType = ItemNamingType.RANDOM;
		}
		//
		if(prefix != null) {
			strb.append(prefix);
			this.prefixLength = prefix.length();
			if(length > prefix.length()) {
				this.length = length;
			} else {
				throw new DanShootHisFootException("Id length should be more than prefix length");
			}
		} else {
			prefixLength = 0;
			if(length > 0){
				this.length = length;
			} else {
				throw new DanShootHisFootException("Id length should be more than 0");
			}
		}
		//
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			throw new DanShootHisFootException("Invalid radix: " + radix);
		}
		this.radix = radix;
		// xorShift(0) = 0, so override this behaviour (which is by default)
		if(ItemNamingType.RANDOM.equals(namingType) && offset == 0) {
			this.lastValue = Long.reverse(currentTimeMillis()) ^ Long.reverseBytes(nanoTime());
		} else {
			this.lastValue = offset;
		}
	}
	//
	/**
	 INTENDED ONLY FOR SEQUENTIAL USE. NOT THREAD-SAFE!
	 @return next id
	 */
	@Override
	public String get() {
		// reset the string buffer
		strb.setLength(prefixLength);
		// calc next number
		switch(namingType) {
			case RANDOM:
				lastValue = Math.abs(MathUtil.xorShift(lastValue));
				break;
			case ASC:
				if(lastValue < Long.MAX_VALUE) {
					lastValue ++;
				} else {
					lastValue = 0;
				}
				break;
			case DESC:
				if(lastValue > 0) {
					lastValue --;
				} else {
					lastValue = Long.MAX_VALUE;
				}
				break;
		}
		//
		final String numStr = Long.toString(lastValue, radix);
		final int nZeros = length - prefixLength - numStr.length();
		if(nZeros > 0) {
			for(int i = 0; i < nZeros; i ++) {
				strb.append('0');
			}
			strb.append(numStr);
		} else if(nZeros < 0) {
			strb.append(numStr.substring(-nZeros));
		} else {
			strb.append(numStr);
		}
		return strb.toString();
	}

	@Override
	public final long getAsLong() {
		return lastValue;
	}
}
