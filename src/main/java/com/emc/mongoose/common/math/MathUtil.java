package com.emc.mongoose.common.math;
/**
 Created by kurila on 18.09.15.
 */
public abstract class MathUtil {
	/** Greatest common divisor */
	public static int gcd(final int x, final int y) {
		int z = 0;
		if(x > y) {
			for(int i = y; i > 0; i --) {
				if(x % i == 0 && y % i == 0) {
					z = i;
					break;
				}
			}
		} else if(x < y) {
			for(int i = x; i > 0; i --) {
				if(x % i == 0 && y % i == 0) {
					z = i;
					break;
				}
			}
		} else {
			z = x;
		}
		return z;
	}
}
