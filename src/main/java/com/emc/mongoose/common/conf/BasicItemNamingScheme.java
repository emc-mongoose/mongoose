package com.emc.mongoose.common.conf;
//
import com.emc.mongoose.common.math.MathUtil;
import com.emc.mongoose.common.net.ServiceUtil;
//
import com.emc.mongoose.common.conf.ItemNamingScheme;
/**
 Created by kurila on 18.12.15.
 */
public class BasicItemNamingScheme
implements ItemNamingScheme {
	//
	protected final ItemNamingScheme.Type type;
	protected long lastSeed;
	//
	public BasicItemNamingScheme(final ItemNamingScheme.Type type) {
		this.type = type;
		switch(type) {
			case RANDOM:
				lastSeed = Math.abs(
					Long.reverse(System.currentTimeMillis()) ^
					Long.reverseBytes(System.nanoTime()) ^
					ServiceUtil.getHostAddrCode()
				);
				break;
			case ASC:
				lastSeed = 0;
				break;
			case DESC:
				lastSeed = Long.MAX_VALUE;
				break;
		}
	}
	/**
	 INTENDED ONLY FOR SEQUENTIAL USE. NOT THREAD-SAFE!
	 @return next id
	 */
	@Override
	public long getNext() {
		final long next = lastSeed;
		switch(type) {
			case RANDOM:
				lastSeed = Math.abs(MathUtil.xorShift(lastSeed ^ System.nanoTime()));
				break;
			case ASC:
				lastSeed ++;
				break;
			case DESC:
				lastSeed --;
				break;
		}
		return next;
	}
}
