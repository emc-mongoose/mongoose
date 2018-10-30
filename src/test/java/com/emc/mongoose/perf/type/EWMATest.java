package com.emc.mongoose.perf.type;

import com.emc.mongoose.metrics.type.EWMA;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static java.lang.Math.exp;

/**
 @author veronika K. on 30.10.18 */
public class EWMATest {

	private final long intervalSecs = 1;
	private final long ps = 10;

	@Test
	public void test() {
		final EWMA ewma = new EWMA(1 - exp(- intervalSecs / ps), intervalSecs, TimeUnit.SECONDS);
	}
}
