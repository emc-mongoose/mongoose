package com.emc.mongoose.common.conf;
//
import static com.emc.mongoose.common.conf.AppConfig.ItemNamingType;
//
import com.emc.mongoose.common.math.MathUtil;
import com.emc.mongoose.common.net.ServiceUtil;
/**
 Created by kurila on 18.12.15.
 */
public class BasicItemIdGenerator
implements ItemIdGenerator {
	//
	protected AppConfig.ItemNamingType namingType;
	protected long lastValue;
	//
	public BasicItemIdGenerator(final long initialValue, final ItemNamingType type) {
		this.namingType = type;
		this.lastValue = initialValue;
	}
	//
	public BasicItemIdGenerator(final ItemNamingType type) {
		this(
			ItemNamingType.ASC.equals(type) ?
				0 :
				ItemNamingType.DESC.equals(type) ?
					Long.MAX_VALUE :
					Math.abs(
						Long.reverse(System.currentTimeMillis()) ^
						Long.reverseBytes(System.nanoTime()) ^
						ServiceUtil.getHostAddrCode()
					),
			type
		);
	}
	//
	/**
	 INTENDED ONLY FOR SEQUENTIAL USE. NOT THREAD-SAFE!
	 @return next id
	 */
	@Override
	public long get() {
		switch(namingType) {
			case RANDOM:
				return lastValue = Math.abs(MathUtil.xorShift(lastValue ^ System.nanoTime()));
			case ASC:
				return lastValue ++;
			case DESC:
				return lastValue --;
			default:
				return 0;
		}
	}
}
