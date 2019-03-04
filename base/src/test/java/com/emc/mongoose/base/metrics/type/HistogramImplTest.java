package com.emc.mongoose.base.metrics.type;

import static org.junit.Assert.assertEquals;

import com.emc.mongoose.base.metrics.snapshot.HistogramSnapshot;
import com.emc.mongoose.base.metrics.util.ConcurrentSlidingWindowLongReservoir;
import java.util.stream.LongStream;
import org.junit.Test;

/** @author veronika K. on 16.10.18 */
public class HistogramImplTest {

	@Test
	public void quantileTest() {
		final long[] srcData = new long[]{
				450, 9, 400, 36, 225, 72, 360, 56, 180, 600, 21, 162, 150, 320, 160, 270, 162, 210, 60,
				504, 175, 150, 80, 200, 48, 180, 18, 80, 84, 126, 30, 32, 216, 63, 640, 36, 200, 45, 300,
				90, 108, 135, 30, 216, 96, 180, 12, 90, 180, 240, 108, 560, 50, 105, 144, 240, 120, 560,
				18, 18, 180, 432, 30, 60, 630, 5, 210, 150, 48, 216, 560, 9, 90, 210, 360, 42, 81, 75, 72,
				56, 112, 280, 192, 160, 48, 108, 98, 192, 144, 49, 40, 60, 160, 45, 300, 48, 14, 144, 168,
				96,
		};
		final LongMeter<HistogramSnapshot> histogram = new HistogramImpl(new ConcurrentSlidingWindowLongReservoir(100));
		LongStream.of(srcData).forEach(histogram::update);
		final HistogramSnapshot snapshot = histogram.snapshot();
		assertEquals(5, snapshot.quantile(0.0)); // -> minimum
		assertEquals(5, snapshot.quantile(0.001));
		assertEquals(30, snapshot.quantile(0.1));
		assertEquals(48, snapshot.quantile(0.2));
		assertEquals(56, snapshot.quantile(0.25));
		assertEquals(126, snapshot.quantile(0.5)); // -> median
		assertEquals(210, snapshot.quantile(0.75));
		assertEquals(225, snapshot.quantile(0.8));
		assertEquals(400, snapshot.quantile(0.9));
		assertEquals(560, snapshot.quantile(0.95));
		assertEquals(640, snapshot.quantile(0.99));
		assertEquals(640, snapshot.quantile(0.999));
	}
}
