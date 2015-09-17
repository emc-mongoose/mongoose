package com.emc.mongoose.storage.mock.impl.base;
//
import com.codahale.metrics.Clock;
import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.log.LogUtil;
import com.emc.mongoose.common.log.Markers;
import com.emc.mongoose.common.net.ServiceUtils;
//
//
import com.emc.mongoose.core.api.load.model.metrics.IOStats;
import com.emc.mongoose.core.impl.load.model.metrics.CustomJmxReporter;
import com.emc.mongoose.core.impl.load.model.metrics.CustomMeter;
import com.emc.mongoose.core.impl.load.model.metrics.CustomMetricRegistry;
import com.emc.mongoose.core.impl.load.model.metrics.ResumableUserTimeClock;
import com.emc.mongoose.storage.mock.api.StorageMock;
import com.emc.mongoose.storage.mock.api.StorageIOStats;
//
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 13.05.15.
 */
public final class BasicStorageIOStats
extends Thread
implements StorageIOStats {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final CustomMetricRegistry metricRegistry = new CustomMetricRegistry();
	private final CustomJmxReporter jmxReporter;
	private final Clock clock = new ResumableUserTimeClock();
	private final Counter
		countFailWrite = metricRegistry.counter(
			MetricRegistry.name(
				StorageMock.class, IOType.WRITE.name(), IOStats.METRIC_NAME_FAIL
			)
		),
		countFailRead = metricRegistry.counter(
			MetricRegistry.name(
				StorageMock.class, IOType.READ.name(), IOStats.METRIC_NAME_FAIL
			)
		),
		countFailDelete = metricRegistry.counter(
			MetricRegistry.name(
				StorageMock.class, IOType.DELETE.name(), IOStats.METRIC_NAME_FAIL
			)
		),
		countContainers = metricRegistry.counter(
			MetricRegistry.name(StorageMock.class, METRIC_NAME_CONTAINERS)
		);
	private final CustomMeter tpWrite, tpRead, tpDelete, bwWrite, bwRead;
	//
	private final long updatePeriodMilliSec;
	private final StorageMock storage;
	//
	public BasicStorageIOStats(
		final StorageMock storage, final int metricsPeriodSec, final boolean jmxServeFlag
	) {
		super(BasicStorageIOStats.class.getSimpleName());
		setDaemon(true);
		updatePeriodMilliSec = TimeUnit.SECONDS.toMillis(metricsPeriodSec);
		tpWrite = metricRegistry.register(
			MetricRegistry.name(
				StorageMock.class, IOType.WRITE.name(), IOStats.METRIC_NAME_TP
			),
			new CustomMeter(clock, metricsPeriodSec)
		);
		tpRead = metricRegistry.register(
			MetricRegistry.name(
				StorageMock.class, IOType.READ.name(), IOStats.METRIC_NAME_TP
			),
			new CustomMeter(clock, metricsPeriodSec)
		);
		tpDelete = metricRegistry.register(
			MetricRegistry.name(
				StorageMock.class, IOType.DELETE.name(), IOStats.METRIC_NAME_TP
			),
			new CustomMeter(clock, metricsPeriodSec)
		);
		bwWrite = metricRegistry.register(
			MetricRegistry.name(
				StorageMock.class, IOType.WRITE.name(), IOStats.METRIC_NAME_BW
			),
			new CustomMeter(clock, metricsPeriodSec)
		);
		bwRead = metricRegistry.register(
			MetricRegistry.name(
				StorageMock.class, IOType.READ.name(), IOStats.METRIC_NAME_BW
			),
			new CustomMeter(clock, metricsPeriodSec)
		);
		this.storage = storage;
		//
		if(jmxServeFlag) {
			final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
				RunTimeConfig.getContext().getRemotePortMonitor()
			);
			jmxReporter = CustomJmxReporter.forRegistry(metricRegistry)
				.convertDurationsTo(TimeUnit.SECONDS)
				.convertRatesTo(TimeUnit.SECONDS)
				.registerWith(mBeanServer)
				.build();
		} else {
			jmxReporter = null;
		}
	}
	//
	private final static String
		MSG_FMT_METRICS = "Capacity used: %d (%.1f%%), containers count: %d\n" +
			"\tOperation |Count       |Failed      |TP[s^-1]avg |TP[s^-1]last|BW[MB*s^-1]avg |BW[MB*s^-1]last\n" +
			"\t----------|------------|------------|------------|------------|---------------|---------------\n" +
			"\tWrite     |%12d|%12d|%12.3f|%12.3f|%15.3f|%15.3f\n" +
			"\tRead      |%12d|%12d|%12.3f|%12.3f|%15.3f|%15.3f\n" +
			"\tDelete    |%12d|%12d|%12.3f|%12.3f|               |";
	//
	@Override
	public final String toString() {
		final long countTotal = storage.getSize();
		return String.format(
			LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
			//
			countTotal, 100.0 * countTotal / storage.getCapacity(), countContainers.getCount(),
			//
			tpWrite.getCount(), countFailWrite.getCount(),
			tpWrite.getMeanRate(), tpWrite.getLastRate(),
			bwWrite.getMeanRate() / IOStats.MIB, bwWrite.getLastRate() / IOStats.MIB,
			//
			tpRead.getCount(), countFailRead.getCount(),
			tpRead.getMeanRate(), tpRead.getLastRate(),
			bwRead.getMeanRate() / IOStats.MIB, bwRead.getLastRate() / IOStats.MIB,
			//
			tpDelete.getCount(), countFailDelete.getCount(),
			tpDelete.getMeanRate(), tpDelete.getLastRate()
		);
	}
	//
	@Override
	public final void start() {
		LOG.debug(Markers.MSG, "Start");
		if(jmxReporter != null) {
			jmxReporter.start();
		}
		super.start();
	}
	//
	@Override
	public final void run() {
		LOG.debug(Markers.MSG, "Running");
		try {
			while(updatePeriodMilliSec > 0 && !isInterrupted()) {
				LOG.info(Markers.MSG, toString());
				Thread.sleep(updatePeriodMilliSec);
			}
		} catch(final InterruptedException ignored) {
			LOG.debug(Markers.MSG, "Interrupted");
		} catch(final Exception e) {
			LogUtil.exception(LOG, Level.WARN, e, "Failure");
		} finally {
			close();
		}
	}
	//
	@Override
	public final void close() {
		if(!isInterrupted()) {
			interrupt();
		}
		if(jmxReporter != null) {
			try {
				jmxReporter.close();
			} catch(final Exception e) {
				LogUtil.exception(LOG, Level.DEBUG, e, "Closing the metrics reporter failure");
			}
		}
	}
	//
	@Override
	public final void markWrite(final boolean succ, final long size) {
		if(succ) {
			tpWrite.mark();
			bwWrite.mark(size);
		} else {
			countFailWrite.inc();
		}
	}
	//
	@Override
	public final void markRead(final boolean succ, final long size) {
		if(succ) {
			tpRead.mark();
			bwRead.mark(size);
		} else {
			countFailRead.inc();
		}
	}
	//
	@Override
	public final void markDelete(final boolean succ) {
		if(succ) {
			tpDelete.mark();
		} else {
			countFailDelete.inc();
		}
	}
	//
	@Override
	public final void containerCreate() {
		countContainers.inc();
	}
	//
	@Override
	public final void containerDelete() {
		countContainers.dec();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// methods necessary for throttling, perf adaptation, etc
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final double getWriteRate() {
		return tpWrite.getLastRate();
	}
	//
	@Override
	public final double getWriteRateBytes() {
		return bwWrite.getLastRate();
	}
	//
	@Override
	public final double getReadRate() {
		return tpRead.getLastRate();
	}
	//
	@Override
	public final double getReadRateBytes() {
		return bwRead.getLastRate();
	}
	//
	@Override
	public final double getDeleteRate() {
		return tpDelete.getLastRate();
	}
}
