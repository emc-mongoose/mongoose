package com.emc.mongoose.storage.mock.impl.cinderella;
//
import com.codahale.metrics.Counter;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
//
import com.emc.mongoose.common.collections.Cache;
import com.emc.mongoose.common.conf.RunTimeConfig;
import com.emc.mongoose.common.logging.LogUtil;
import com.emc.mongoose.common.net.ServiceUtils;
//
import com.emc.mongoose.core.api.io.task.IOTask;
import com.emc.mongoose.core.api.load.executor.LoadExecutor;
//
import com.emc.mongoose.storage.mock.api.data.WSObjectMock;
import com.emc.mongoose.storage.mock.api.stats.IOStats;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//
import javax.management.MBeanServer;
import java.util.concurrent.TimeUnit;
/**
 Created by kurila on 13.05.15.
 */
public final class BasicWSIOStats
extends Thread
implements IOStats {
	//
	private final static Logger LOG = LogManager.getLogger();
	//
	private final MetricRegistry metricRegistry = new MetricRegistry();
	private final MBeanServer mBeanServer = ServiceUtils.getMBeanServer(
		RunTimeConfig.getContext().getRemotePortExport()
	);
	private final JmxReporter metricsReporter = JmxReporter.forRegistry(metricRegistry)
		.convertDurationsTo(TimeUnit.SECONDS)
		.convertRatesTo(TimeUnit.SECONDS)
		.registerWith(mBeanServer)
		.build();
	private final Counter
		countSuccCreate = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countSuccRead = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countSuccDelete = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.DELETE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_SUCC
			)
		),
		countFailCreate = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_FAIL
			)
		),
		countFailRead = metricRegistry.counter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), METRIC_COUNT,
				LoadExecutor.METRIC_NAME_FAIL
			)
		);
	private final Meter
		bwAll = metricRegistry.meter(
			MetricRegistry.name(Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_BW)
		),
		bwCreate = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_BW
			)
		),
		bwRead = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_BW
			)
		),
		tpAll = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, ALL_METHODS, LoadExecutor.METRIC_NAME_TP
			)
		),
		tpCreate = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.CREATE), LoadExecutor.METRIC_NAME_TP
			)
		),
		tpRead = metricRegistry.meter(
			MetricRegistry.name(
				Cinderella.class, String.valueOf(IOTask.Type.READ), LoadExecutor.METRIC_NAME_TP
			)
		);
	//
	private final long updateMilliPeriod;
	private final Cache<String, WSObjectMock> storage;
	//
	public BasicWSIOStats(
		final RunTimeConfig runTimeConfig, final Cache<String, WSObjectMock> storage
	) {
		super(BasicWSIOStats.class.getSimpleName());
		setDaemon(true);
		updateMilliPeriod = TimeUnit.SECONDS.toMillis(runTimeConfig.getLoadMetricsPeriodSec());
		this.storage = storage;
	}
	//
	private final static String
		MSG_FMT_METRICS = "capacity used=(%d/%.3f%%); count=(succ=(%d/%d/%d); fail=(%d/%d)); " +
			"TP[/s]=(%.3f/%.3f/%.3f/%.3f); BW[MB/s]=(%.3f/%.3f/%.3f/%.3f)";
	//
	@Override
	public final String toString() {
		return String.format(
			LogUtil.LOCALE_DEFAULT, MSG_FMT_METRICS,
			//
			storage.size(), 100.0 * storage.size() / storage.getCapacity(),
			//
			countSuccCreate.getCount(), countSuccRead.getCount(), countSuccDelete.getCount(),
			countFailCreate.getCount(), countFailRead.getCount(),
			//
			tpAll.getMeanRate(),
			tpAll.getOneMinuteRate(),
			tpAll.getFiveMinuteRate(),
			tpAll.getFifteenMinuteRate(),
			//
			bwAll.getMeanRate() / LoadExecutor.MIB,
			bwAll.getOneMinuteRate() / LoadExecutor.MIB,
			bwAll.getFiveMinuteRate() / LoadExecutor.MIB,
			bwAll.getFifteenMinuteRate() / LoadExecutor.MIB
		);
	}
	//
	@Override
	public final void start() {
		LOG.debug(LogUtil.MSG, "Start");
		metricsReporter.start();
		super.start();
	}
	//
	@Override
	public final void run() {
		LOG.debug(LogUtil.MSG, "Running");
		try {
			while(updateMilliPeriod > 0) {
				LOG.info(LogUtil.PERF_AVG, toString());
				Thread.sleep(updateMilliPeriod);
			}
		} catch(final Exception e) {
			LogUtil.failure(LOG, Level.WARN, e, "Failure");
		} finally {
			close();
		}
	}
	//
	@Override
	public final void close() {
		if(isAlive()) {
			interrupt();
		}
		metricsReporter.close();
	}
	//
	@Override
	public final void markCreate(final long size) {
		if(size < 0) {
			countFailCreate.inc();
		} else {
			countSuccCreate.inc();
			tpCreate.mark();
			bwCreate.mark(size);
			tpAll.mark();
			bwAll.mark(size);
		}
	}
	//
	@Override
	public final void markRead(final long size) {
		if(size < 0) {
			countFailRead.inc();
		} else {
			countSuccRead.inc();
			tpRead.mark();
			bwRead.mark(size);
			tpAll.mark();
			bwAll.mark(size);
		}
	}
	//
	@Override
	public final void markDelete() {
		countSuccDelete.inc();
		tpAll.mark();
	}
	////////////////////////////////////////////////////////////////////////////////////////////////
	// methods necessary for throttling, perf adaptation, etc
	////////////////////////////////////////////////////////////////////////////////////////////////
	@Override
	public final double getMeanRate() {
		return tpAll.getOneMinuteRate();
	}
	//
	@Override
	public final double getWriteRate() {
		return tpCreate.getOneMinuteRate();
	}
	//
	@Override
	public final double getReadRate() {
		return tpRead.getOneMinuteRate();
	}
	//
	@Override
	public final double getWriteRateBytes() {
		return bwCreate.getOneMinuteRate();
	}
	//
	@Override
	public final double getReadRateBytes() {
		return bwRead.getOneMinuteRate();
	}
}
