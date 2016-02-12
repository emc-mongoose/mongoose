package com.emc.mongoose.core.impl.item.base;
//
import com.emc.mongoose.common.conf.BasicValueGenerator;
import com.emc.mongoose.common.math.MathUtil;
//
import com.emc.mongoose.common.net.ServiceUtil;
import com.emc.mongoose.core.api.item.base.ItemNamingType;

import java.util.concurrent.Callable;
/**
 Created by kurila on 18.12.15.
 */
public class BasicItemNameGenerator
extends BasicValueGenerator<String> {
	//
	protected final ItemNamingType namingType;
	protected final String prefix;
	protected final int length, prefixLength, radix;
	protected final StringBuilder strb = new StringBuilder();
	//
	protected long lastValue;
	//
	private final class UpdateAction
	implements Callable<Long> {
		@Override
		public Long call()
		throws Exception {
			return lastValue ++;
		}
	}
	//
	public BasicItemNameGenerator(
		final ItemNamingType namingType, final String prefix, final int length, final int radix,
		final long offset
	) throws IllegalArgumentException {
		//
		super(null, null);
		//
		if(namingType != null) {
			this.namingType = namingType;
		} else {
			this.namingType = ItemNamingType.RANDOM;
		}
		//
		this.prefix = prefix;
		//
		if(prefix != null) {
			this.prefixLength = prefix.length();
			if(length > prefix.length()) {
				this.length = length;
			} else {
				throw new IllegalArgumentException("Id length should be more than prefix length");
			}

		} else {
			prefixLength = 0;
			if(length > 0){
				this.length = length;
			} else {
				throw new IllegalArgumentException("Id length should be more than 0");
			}
		}
		//
		if(radix < Character.MIN_RADIX || radix > Character.MAX_RADIX) {
			throw new IllegalArgumentException("Invalid radix: " + radix);
		}
		this.radix = radix;
		// xorShift(0) = 0, so override this behaviour (which is by default)
		if(ItemNamingType.RANDOM.equals(namingType) && offset == 0) {
			this.lastValue = Math.abs(
				Long.reverse(System.currentTimeMillis()) ^
				Long.reverseBytes(System.nanoTime()) ^
				ServiceUtil.getHostAddrCode()
			);
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
				lastValue = Math.abs(MathUtil.xorShift(lastValue ^ System.nanoTime()));
			case ASC:
				lastValue ++;
			case DESC:
				lastValue --;
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
			strb.append(numStr.substring(nZeros - 1));
		} else {
			strb.append(numStr);
		}
		return strb.toString();
	}
	// yes, this is very ugly bandage
	public long getLastValue() {
		return lastValue;
	}
}
