package com.emc.mongoose.metrics.type;

import com.emc.mongoose.metrics.snapshot.HistogramSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshot;
import com.emc.mongoose.metrics.snapshot.TimingMetricSnapshotImpl;

import java.util.concurrent.atomic.LongAdder;

/**
 @author veronika K. on 10.10.18 */
public class TimingMeterImpl
implements LongMeter<TimingMetricSnapshot> {

	private final LongMeter<HistogramSnapshot> histogram;
	private final LongAdder count = new LongAdder();
	private final LongAdder sum = new LongAdder();
	private volatile long min = Long.MAX_VALUE;
	private volatile long max = Long.MIN_VALUE;
	private final String metricName;

	public TimingMeterImpl(final LongMeter<HistogramSnapshot> histogram, final String metricName) {
		this.histogram = histogram;
		this.metricName = metricName;
	}

	@Override
	public void update(final long value) {
		histogram.update(value);
		count.increment();
		sum.add(value);
		if(value < min) {
			min = value;
		}
		if(value > max) {
			max = value;
		}
	}

	@Override
	public TimingMetricSnapshotImpl snapshot() {
		if(count.sum() == 0) {
			return new TimingMetricSnapshotImpl(0, 0, 0, 0, 0, histogram.snapshot(), "");
		}
		return new TimingMetricSnapshotImpl(
			sum.sum(), count.sum(), min, max, ((double) sum.sum()) / count.sum(), histogram.snapshot(), metricName
		);
	}
}
